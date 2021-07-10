import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import JsonUtility.JsonHandler;
import Message.*;
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
		this.jsonHandler = new JsonHandler();
		this.requestFilePath = "src/main/resources/Server/requests_Server_" + id + ".json";
		this.incomingMessages = incomingMessages;
		this.name = name;
		this.socket = socket;
		this.server = server;
		this.requestList = new ArrayList<>();
	}
	
	public void run() {
		logger.info("Starting <" + name + "> for port <" + socket.getLocalPort() + ">...");
		online = true;
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
					Message infoMsgCar = new Message(StatusTypes.INFO, msg.getSenderAddress(), msg.getSenderPort(), "0", "GetInitialInfo");
					DatagramPacket packetCar = new DatagramPacket(infoMsgCar.toString().getBytes(), infoMsgCar.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
					logger.trace("<" + name + "> sent: <"+ new String(packetCar.getData(), 0, packetCar.getLength()) +">");
					socket.send(packetCar);
					Message infoMsgHotel = new Message(StatusTypes.INFO, msg.getSenderAddress(), msg.getSenderPort(), "0", "GetInitialInfo");
					DatagramPacket packetHotel = new DatagramPacket(infoMsgHotel.toString().getBytes(), infoMsgHotel.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
					logger.trace("<" + name + "> sent: <"+ new String(packetHotel.getData(), 0, packetHotel.getLength()) +">");
					socket.send(packetHotel);
					response = null;
					break;
				case BOOKING:
					if(msg.getStatusMessageEndTime() > msg.getStatusMessageStartTime()) {
						String uniqueID = UUID.randomUUID().toString();
						String timestamp = String.valueOf(new Date().getTime());
						String newBookingID = server.getName()+ "_" + timestamp + "_" + uniqueID;
						response = new Message(StatusTypes.BOOKING, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						Message prepareMsgCar = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) +">");
						socket.send(preparePacketCar);
						Message prepareMsgHotel = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) +">");
						socket.send(preparePacketHotel);
						this.addRequestToList(newBookingID, Integer.parseInt(msg.getStatusMessageCarId()), Integer.parseInt(msg.getStatusMessageRoomId()), new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()), msg.getSenderAddress(), msg.getSenderPort());
					} else {
						response = new Message(StatusTypes.ERROR, InetAddress.getLocalHost(), socket.getLocalPort(), null, "ERROR_Invalid_Booking");
					}
					break;
				case READY:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						logger.info("CARBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.READY, null);
						if(request.bothReady()) {
							logger.info("RESULT => COMMIT!");
							request.resetMessageCounter();
							
							Message answerForHotelBroker = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message commitClient = new Message(StatusTypes.COMMIT, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
							DatagramPacket commitPacketClient = new DatagramPacket(commitClient.toString().getBytes(), commitClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(commitPacketClient.getData(), 0, commitPacketClient.getLength()) +">");
							socket.send(commitPacketClient);
							
							response = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
						} else if(request.getHotelBrokerState().equals(StatusTypes.ABORT) && request.getMessageCounter() == 2) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							
							Message answerForHotelBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsAlreadyBlocked");
							DatagramPacket rollbackPacketClient = new DatagramPacket(rollbackClient.toString().getBytes(), rollbackClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(rollbackPacketClient.getData(), 0, rollbackPacketClient.getLength()) +">");
							socket.send(rollbackPacketClient);
							
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
						}  else {
							response = null;
						}
					}
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						logger.info("HOTELBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.READY);
						if(request.bothReady()) {
							logger.info("RESULT => COMMIT!");
							request.resetMessageCounter();
							
							Message answerForCarBroker = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message commitClient = new Message(StatusTypes.COMMIT, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
							DatagramPacket commitPacketClient = new DatagramPacket(commitClient.toString().getBytes(), commitClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(commitPacketClient.getData(), 0, commitPacketClient.getLength()) +">");
							socket.send(commitPacketClient);
							
							response = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
						} else if(request.getCarBrokerState().equals(StatusTypes.ABORT) && request.getMessageCounter() == 2) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							
							Message answerForCarBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), "CarIsAlreadyBlocked_" + msg.getStatusMessage());
							DatagramPacket rollbackPacketClient = new DatagramPacket(rollbackClient.toString().getBytes(), rollbackClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(rollbackPacketClient.getData(), 0, rollbackPacketClient.getLength()) +">");
							socket.send(rollbackPacketClient);
							
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
						} else {
							response = null;
						}
					}
					break;
				case ABORT:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						logger.info("CARBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ABORT, null);
						if(request.getHotelBrokerState().equals(StatusTypes.ABORT) || request.getHotelBrokerState().equals(StatusTypes.READY)) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForHotelBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message rollbackClient = new Message("");
							if(request.getHotelBrokerState().equals(StatusTypes.READY)) {
								rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsFree");
							}
							if(request.getHotelBrokerState().equals(StatusTypes.ABORT)) {
								rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage() + "_HotelRoomIsAlreadyBlocked");
							}
							DatagramPacket rollbackPacketClient = new DatagramPacket(rollbackClient.toString().getBytes(), rollbackClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(rollbackPacketClient.getData(), 0, rollbackPacketClient.getLength()) +">");
							socket.send(rollbackPacketClient);
							
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
						}
					} else {
						response = null;
					}
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						logger.info("HOTELBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ABORT);
						if(request.getCarBrokerState().equals(StatusTypes.ABORT) || request.getCarBrokerState().equals(StatusTypes.READY)) {
							logger.info("RESULT => ROLLBACK!");
							request.resetMessageCounter();
							Message answerForCarBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							
							Message rollbackClient = new Message("");
							if(request.getCarBrokerState().equals(StatusTypes.READY)) {
								rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), "CarIsFree_" + msg.getStatusMessage());
							}
							if(request.getCarBrokerState().equals(StatusTypes.ABORT)) {
								rollbackClient = new Message(StatusTypes.ROLLBACK, InetAddress.getLocalHost(), socket.getLocalPort(), msg.getBookingID(), "CarIsAlreadyBlocked_" + msg.getStatusMessage());
							}
							DatagramPacket rollbackPacketClient = new DatagramPacket(rollbackClient.toString().getBytes(), rollbackClient.toString().getBytes().length, request.getClientAddress(), request.getClientPort());
							logger.trace("<" + name + "> sent: <"+ new String(rollbackPacketClient.getData(), 0, rollbackPacketClient.getLength()) +">");
							socket.send(rollbackPacketClient);
							
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
						}
					} else {
						response = null;
					}
					break;
				case INQUIRE:					
					//resend COMMIT
					if(request.getCarBrokerState().equals(StatusTypes.READY) && request.getHotelBrokerState().equals(StatusTypes.READY)) {
						response = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
						logger.trace("<" + name + "> resent: <"+ response.toString() +">");
					}
					//resend ROLLBACK
					if(request.getCarBrokerState().equals(StatusTypes.ABORT) || request.getHotelBrokerState().equals(StatusTypes.ABORT)) {
						response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
						logger.trace("<" + name + "> resent: <"+ response.toString() +">");
					}
					//##########################################
					//What happens if one broker is still on initialize (offline)?
					break;
				case ACKNOWLEDGMENT:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ACKNOWLEDGMENT, null);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
							logger.info("FINISHED 2PC");
						}
					}
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ACKNOWLEDGMENT);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
							logger.info("FINISHED 2PC");
						}
					}
					response = null;
					break;
				case ERROR:
					response = null;
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(StatusTypes.CONNECTIONTEST, InetAddress.getLocalHost(), socket.getLocalPort(), "0", "InitialMessageResponseServerMessageHandler");
					}
					break;					
				default:
					response = new Message(StatusTypes.ERROR, InetAddress.getLocalHost(), socket.getLocalPort(), null, "ERROR_ID_FormatException");
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
					if(carState != null && !StatusTypes.valueOf(singleRequest.get("CarState").toString()).equals(carState)) {
						singleRequest.replace("CarState", carState.toString());
						updated = true;
					}
					if(hotelState != null && !StatusTypes.valueOf(singleRequest.get("HotelState").toString()).equals(hotelState)) {
						singleRequest.replace("HotelState", hotelState.toString());
						updated = true;
					}
					if(updated) {
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
}