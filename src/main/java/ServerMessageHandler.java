import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import JsonUtility.JsonHandler;
import Message.*;
import Request.CarRequest;
import Request.ServerRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerMessageHandler implements Runnable{
	//Attribute
	private final int id;
	private MessageFactory msgFactory;
	private final String requestFilePath;
	private final JsonHandler jsonHandler;
	private static final Logger logger = LogManager.getRootLogger();
	private final DatagramSocket socket;
	private final BlockingQueue<Message> incomingMessages;
	private final String name;
	private Boolean online;
	private final Server server;
	private final ArrayList<ServerRequest> requestList;
	
	public ServerMessageHandler(int id, String name, BlockingQueue<Message> incomingMessages, DatagramSocket socket, Server server) {
		this.id = id;
		this.msgFactory = new MessageFactory();
		this.jsonHandler = new JsonHandler();
		this.requestFilePath = "src/main/resources/Server/requests_Server_" + id + ".json";
		this.incomingMessages = incomingMessages;
		this.name = name;
		this.socket = socket;
		this.server = server;
		this.requestList = new ArrayList<>();
		this.initialize();
	}
	
	public void run() {
		logger.info("Starting <" + name + "> for port <" + socket.getLocalPort() + ">...");
		online = true;
		ServerMessageHandlerTimeChecker smhtc = new ServerMessageHandlerTimeChecker(this);
		while (online) {
        	try {
				Message inMsg = incomingMessages.take();
				logger.info("<" + name + "> removed Message from Queue: <"+ inMsg.toString() +">");
				Message outMsg = this.analyzeAndGetResponse(inMsg);
				if(outMsg != null) {
					DatagramPacket packet = new DatagramPacket(outMsg.toString().getBytes(), outMsg.toString().getBytes().length, inMsg.getSenderAddress(), inMsg.getSenderPort());
					logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
					socket.send(packet);
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
        socket.close();
	}
	
	private Message analyzeAndGetResponse(Message msg) {
		String statusMessage = msg.getStatusMessage();
		Message response = null;
		ServerRequest request = this.getRequest(msg.getBookingID());
		try {
			switch(msg.getStatus()) {
				case INFO:
					logger.info("Handling Info message");
					Message infoMsgCar = msgFactory.buildInfo("0", "GetInitialInfo", msg.getSenderAddress(), msg.getSenderPort());
					answerParticipant(infoMsgCar, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
					Message infoMsgHotel = msgFactory.buildInfo("0", "GetInitialInfo", msg.getSenderAddress(), msg.getSenderPort());
					answerParticipant(infoMsgHotel, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
					break;
				case BOOKING:
					if(msg.getStatusMessageEndTime() > msg.getStatusMessageStartTime()) {
						String uniqueID = UUID.randomUUID().toString();
						String timestamp = String.valueOf(new Date().getTime());
						String newBookingID = server.getName()+ "_" + timestamp + "_" + uniqueID;
						response = msgFactory.buildBooking(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						Message prepareMsgCar = msgFactory.buildPrepare(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						answerParticipant(prepareMsgCar, server.getCarBroker().getAddress(),server.getCarBroker().getPort());
						Message prepareMsgHotel = msgFactory.buildPrepare(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						answerParticipant(prepareMsgHotel, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
						this.addRequestToList(newBookingID, Integer.parseInt(msg.getStatusMessageCarId()), Integer.parseInt(msg.getStatusMessageRoomId()), new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()), msg.getSenderAddress(), msg.getSenderPort());
					} else {
						response = msgFactory.buildError(null, "ERROR_Invalid_Booking", InetAddress.getLocalHost(), socket.getLocalPort());
					}
					break;
				case READY:
					if(messageFromCarBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						logger.info("CARBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.READY, null);
						if(request.bothReady()) {
							logger.info("RESULT => COMMIT!");
							request.resetMessageCounter();
							Message answerForHotelBroker = msgFactory.buildCommit(msg.getBookingID(), "OkThanBook", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
							Message commitClient = msgFactory.buildCommit(msg.getBookingID(), msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
							answerParticipant(commitClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildCommit(msg.getBookingID(), "OkThanBook", this.socket.getLocalAddress(), this.socket.getLocalPort());
						} else if(request.getHotelBrokerState().equals(StatusTypes.ABORT) && request.getMessageCounter() >= 2) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForHotelBroker = msgFactory.buildRollback(msg.getBookingID(), "OkThanRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
							Message rollbackClient = msgFactory.buildRollback(msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsAlreadyBlocked", InetAddress.getLocalHost(), socket.getLocalPort());
							answerParticipant(rollbackClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildRollback(msg.getBookingID(), "OkThanRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						}
					}
					if(messageFromHotelBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						logger.info("HOTELBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.READY);
						if(request.bothReady()) {
							logger.info("RESULT => COMMIT!");
							request.resetMessageCounter();
							Message answerForCarBroker = msgFactory.buildCommit(msg.getBookingID(), "OkThanBook", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
							Message commitClient = msgFactory.buildCommit(msg.getBookingID(), msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
							answerParticipant(commitClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildCommit(msg.getBookingID(), "OkThanBook", this.socket.getLocalAddress(), this.socket.getLocalPort());
						} else if(request.getCarBrokerState().equals(StatusTypes.ABORT) && request.getMessageCounter() >= 2) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForCarBroker = msgFactory.buildRollback(msg.getBookingID(), "OkThanRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
							Message rollbackClient = msgFactory.buildRollback(msg.getBookingID(), "CarIsAlreadyBlocked_" + msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
							answerParticipant(rollbackClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildRollback(msg.getBookingID(), "OkThanRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						}
					}
					break;
				case ABORT:
					if(messageFromCarBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						logger.info("CARBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ABORT, null);
						if(request.getHotelBrokerState().equals(StatusTypes.ABORT) || request.getHotelBrokerState().equals(StatusTypes.READY)) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForHotelBroker = msgFactory.buildRollback(msg.getBookingID(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
							Message rollbackClient = msgFactory.buildEmpty("");
							if(request.getHotelBrokerState().equals(StatusTypes.READY)) {
								rollbackClient = msgFactory.buildRollback(msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsFree", InetAddress.getLocalHost(), socket.getLocalPort());
							}
							if(request.getHotelBrokerState().equals(StatusTypes.ABORT)) {
								rollbackClient = msgFactory.buildRollback(msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsAlreadyBlocked", InetAddress.getLocalHost(), socket.getLocalPort());
							}
							answerParticipant(rollbackClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildRollback(msg.getBookingID(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						}
					}
					if(messageFromHotelBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						logger.info("HOTELBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ABORT);
						if(request.getCarBrokerState().equals(StatusTypes.ABORT) || request.getCarBrokerState().equals(StatusTypes.READY)) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForCarBroker = msgFactory.buildRollback(msg.getBookingID(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(answerForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
							Message rollbackClient = msgFactory.buildEmpty("");
							if(request.getCarBrokerState().equals(StatusTypes.READY)) {
								rollbackClient = msgFactory.buildRollback(msg.getBookingID(), "CarIsFree_" + msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
							}
							if(request.getCarBrokerState().equals(StatusTypes.ABORT)) {
								rollbackClient = msgFactory.buildRollback(msg.getBookingID(), "CarIsAlreadyBlocked_" + msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
							}
							answerParticipant(rollbackClient, request.getClientAddress(), request.getClientPort());
							response = msgFactory.buildRollback(msg.getBookingID(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						}
					}
					break;
				case INQUIRE:					
					//resend COMMIT
					if(request.getCarBrokerState().equals(StatusTypes.READY) && request.getHotelBrokerState().equals(StatusTypes.READY)) {
						response = msgFactory.buildCommit(msg.getBookingID(), "OkThanBook", this.socket.getLocalAddress(), this.socket.getLocalPort());
					}
					//resend ROLLBACK
					if(request.getCarBrokerState().equals(StatusTypes.ABORT) || request.getHotelBrokerState().equals(StatusTypes.ABORT)) {
						response = msgFactory.buildRollback(msg.getBookingID(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
					}
					break;
				case ACKNOWLEDGMENT:
					if(messageFromCarBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ACKNOWLEDGMENT, null);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
							logger.info("FINISHED 2PC");
						}
					}
					if(messageFromHotelBroker(msg.getSenderAddress(), msg.getSenderPort())) {
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ACKNOWLEDGMENT);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
							logger.info("FINISHED 2PC");
						}
					}
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = msgFactory.buildConnectionTest("0", "InitialMessageResponseServerMessageHandler", InetAddress.getLocalHost(), socket.getLocalPort());
					}
					break;					
				default:
					response = msgFactory.buildError(null, "ERROR_ID_FormatException", InetAddress.getLocalHost(), socket.getLocalPort());
					break;
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private void addRequestToList(String bookingId, int carId, int roomId, Date startTime, Date endTime, InetAddress clientAddress, int clientPort) {
		this.requestList.add(new ServerRequest(bookingId, carId, roomId, startTime, endTime, clientAddress, clientPort));
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(this.requestFilePath))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject requestsData = (JSONObject) jsonContent;
			Object serverRequestDataContent = requestsData.get("ServerRequests");
			JSONArray serverRequests = (JSONArray) serverRequestDataContent;
			JSONObject serverRequest = new JSONObject();
			serverRequest.put("BookingId", bookingId);
			serverRequest.put("CarId", carId);
			serverRequest.put("RoomId", roomId);
			serverRequest.put("StartTime", startTime.getTime());
			serverRequest.put("EndTime", endTime.getTime());
			serverRequest.put("ClientAddress", clientAddress.getHostAddress());
			serverRequest.put("ClientPort", clientPort);
			serverRequest.put("CarState", StatusTypes.INITIALIZED.toString());
			serverRequest.put("HotelState", StatusTypes.INITIALIZED.toString());
			serverRequests.add(serverRequest);
			try (FileWriter file = new FileWriter(this.requestFilePath)) {
				file.write(requestsData.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	private void updateRequestAtList(String bookingId, StatusTypes carState, StatusTypes hotelState) {
		ServerRequest request = getRequest(bookingId);
		boolean updated = false;
		if(!(request.getCarBrokerState().equals(carState)) && carState != null) {
			request.setCarBrokerState(carState);
		}
		if(!(request.getHotelBrokerState().equals(hotelState)) && hotelState != null) {
			request.setHotelBrokerState(hotelState);
		}
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(this.requestFilePath)) {
			JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
			JSONArray serverRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("ServerRequests"));
			for(int i = 0; i < serverRequests.size(); i++) {
				JSONObject singleRequest = jsonHandler.getAttributeAsJsonObject(serverRequests.get(i));
				if(singleRequest.get("BookingId").toString().equals(bookingId)) {
					if (carState != null && !StatusTypes.valueOf(singleRequest.get("CarState").toString()).equals(carState)) {
						singleRequest.replace("CarState", carState.toString());
						updated = true;
					}
					if (hotelState != null && !StatusTypes.valueOf(singleRequest.get("HotelState").toString()).equals(hotelState)) {
						singleRequest.replace("HotelState", hotelState.toString());
						updated = true;
					}
					if (updated) {
						break;
					}
				}
			}
			try (FileWriter file = new FileWriter(this.requestFilePath)) {
				file.write(requestsData.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public void removeRequestFromList(String bookingId) {
		for(int i = 0; i < requestList.size(); i++) {
			if(this.requestList.get(i).getId().equals(bookingId)) {
				this.requestList.remove(i);
				break;
			}
		}
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(this.requestFilePath))
		{
			JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
			JSONArray serverRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("ServerRequests"));
			for(int i = 0; i < serverRequests.size(); i++) {
				Object requestData = serverRequests.get(i);
				JSONObject singleRequest = (JSONObject) requestData;
				if(singleRequest.get("BookingId").toString().equals(bookingId)) {
					serverRequests.remove(i);
					break;
				}
			}
			try (FileWriter file = new FileWriter(this.requestFilePath)) {
				file.write(requestsData.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	private void initialize() {
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(requestFilePath))
		{
			JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
			JSONArray requests = jsonHandler.getAttributeAsJsonArray(requestsData.get("ServerRequests"));
			for (int i = 0; i < requests.size(); i++) {
				JSONObject requestInfo = jsonHandler.getAttributeAsJsonObject(requests.get(i));
				ServerRequest singleServerRequest = new ServerRequest(requestInfo.get("BookindId").toString(),
					Integer.parseInt(requestInfo.get("CarId").toString()),
					Integer.parseInt(requestInfo.get("RoomId").toString()),
					new Date(Long.parseLong(requestInfo.get("StartTime").toString())),
					new Date(Long.parseLong(requestInfo.get("EndTime").toString())),
					InetAddress.getByName(requestInfo.get("ClientAddress").toString()),
					Integer.parseInt(requestInfo.get("clientPort").toString()),
					StatusTypes.valueOf(requestInfo.get("CarState").toString()),
					StatusTypes.valueOf(requestInfo.get("HotelState").toString())
				);
				this.requestList.add(singleServerRequest);
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		ServerRequest singleOldRequest;
		if(requestList.size() > 0) {
			for(int i = 0; i < requestList.size(); i++) {
				singleOldRequest = requestList.get(i);
				Message msgForCarBroker = msgFactory.buildInquire(singleOldRequest.getId(), "PlsSendAgain", this.socket.getLocalAddress(), this.socket.getLocalPort());
				answerParticipant(msgForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
				Message msgForHotelBroker = msgFactory.buildInquire(singleOldRequest.getId(), "PlsSendAgain", this.socket.getLocalAddress(), this.socket.getLocalPort());
				answerParticipant(msgForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
			}
		}
	}

	private ServerRequest getRequest(String bookingId) {
		ServerRequest request = null;
		for(int i = 0; i < requestList.size(); i++) {
			if(this.requestList.get(i).getId().equals(bookingId)) {
				request = this.requestList.get(i);
				break;
			}
		}
		return request;
	}

	private void answerParticipant(Message msg, InetAddress clientAddress, int clientPort) {
		DatagramPacket dp = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, clientAddress, clientPort);
		logger.trace("<" + name + "> sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
		try {
			socket.send(dp);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private boolean messageFromCarBroker(InetAddress pAddress, int pPort) {
		boolean result = false;
		if(pAddress.equals(server.getCarBroker().getAddress()) && pPort == server.getCarBroker().getPort()) {
			result = true;
		}
		return result;
	}

	private boolean messageFromHotelBroker(InetAddress pAddress, int pPort) {
		boolean result = false;
		if(pAddress.equals(server.getHotelBroker().getAddress()) && pPort == server.getHotelBroker().getPort()) {
			result = true;
		}
		return result;
	}
}