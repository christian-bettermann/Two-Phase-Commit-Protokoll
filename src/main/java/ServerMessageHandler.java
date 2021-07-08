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
	private int id;
	private static final Logger logger = LogManager.getRootLogger();
	private DatagramSocket socket;
	private BlockingQueue<Message> incomingMessages;
	private String name;
	private Boolean online;
	private Server server;
	private ArrayList<ServerRequest> requestList;
	
	
	public ServerMessageHandler(int id, String name, BlockingQueue<Message> incomingMessages, DatagramSocket socket, Server server) {
		this.id = id;
		this.incomingMessages = incomingMessages;
		this.name = name;
		this.socket = socket;
		this.server = server;
		this.requestList = new ArrayList<ServerRequest>();
	}
	
	public void run() {
		//get messages from queue
		//handle messages
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
		Message response = new Message();
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
						
						//send booking to brokers
						Message prepareMsgCar = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) +">");
						socket.send(preparePacketCar);
						
						Message prepareMsgHotel = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) +">");
						socket.send(preparePacketHotel);
						this.addRequestToList(newBookingID, Integer.parseInt(msg.getStatusMessageCarId()), Integer.parseInt(msg.getStatusMessageRoomId()), new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()));
					} else {
						response = new Message(StatusTypes.ERROR, InetAddress.getLocalHost(), socket.getLocalPort(), null, "ERROR_Invalid_Booking");
					}
					break;
				case READY:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						logger.error("CARBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.READY, null);
						if(this.getRequest(msg.getBookingID()).bothReady()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForHotelBroker = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
						} else if(this.getRequest(msg.getBookingID()).getHotelBrokerState().equals(StatusTypes.ABORT) && this.getRequest(msg.getBookingID()).getMessageCounter() == 2) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForCarBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
						}  else {
							response = null;
						}
					}

					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						logger.error("HOTELBROKER MESSAGE READY!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.READY);
						if(this.getRequest(msg.getBookingID()).bothReady()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForHotelBroker = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.COMMIT, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanBook");
						} else if(this.getRequest(msg.getBookingID()).getCarBrokerState().equals(StatusTypes.ABORT) && this.getRequest(msg.getBookingID()).getMessageCounter() == 2) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForCarBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThanRollback");
						} else {
							response = null;
						}
					}
					break;
				case ABORT:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						logger.error("CARBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ABORT, null);
						if(this.getRequest(msg.getBookingID()).getMessageCounter() == 2) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForHotelBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
							DatagramPacket packet = new DatagramPacket(answerForHotelBroker.toString().getBytes(), answerForHotelBroker.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
						}
					} else {
						response = null;
					}

					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						logger.error("HOTELBROKER MESSAGE ABORT!");
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ABORT);
						if(this.getRequest(msg.getBookingID()).getMessageCounter() == 2) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							Message answerForCarBroker = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
							DatagramPacket packet = new DatagramPacket(answerForCarBroker.toString().getBytes(), answerForCarBroker.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
							logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
							socket.send(packet);
							response = new Message(StatusTypes.ROLLBACK, this.socket.getLocalAddress(), this.socket.getLocalPort(), msg.getBookingID(), "OkThenRollback");
						}
					} else {
						response = null;
					}
					break;
				case ACKNOWLEDGMENT:
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						this.updateRequestAtList(msg.getBookingID(), StatusTypes.ACKNOWLEDGMENT, null);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
						}
					}
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						this.updateRequestAtList(msg.getBookingID(), null, StatusTypes.ACKNOWLEDGMENT);
						if(this.getRequest(msg.getBookingID()).bothAcknowledged()) {
							this.getRequest(msg.getBookingID()).resetMessageCounter();
							this.removeRequestFromList(msg.getBookingID());
						}
					}
					response = null;
					break;
				case TESTING:
					if(statusMessage.equals("HiFromCarBroker") || statusMessage.equals("HiFromHotel")) {
						response = new Message(StatusTypes.TESTING, InetAddress.getLocalHost(), socket.getLocalPort(), "0", "HiFromServerMessageHandler");
					}
					if(statusMessage.equals("OK")) {
						logger.info("Finished test");
						response = null;
					}
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

	private void addRequestToList(String bookingId, int carId, int roomId, Date startTime, Date endTime) {
		this.requestList.add(new ServerRequest(bookingId, carId, roomId, startTime, endTime));
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader("src/main/resources/Server/requests_Server_" + this.id + ".json"))
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
			serverRequest.put("CarState", StatusTypes.INITIALIZED.toString());
			serverRequest.put("HotelState", StatusTypes.INITIALIZED.toString());
			serverRequests.add(serverRequest);
			try (FileWriter file = new FileWriter("src/main/resources/Server/requests_Server_" + this.id + ".json")) {
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
		try (FileReader reader = new FileReader("src/main/resources/Server/requests_Server_" + this.id + ".json"))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject requestsData = (JSONObject) jsonContent;
			Object serverRequestDataContent = requestsData.get("ServerRequests");
			JSONArray serverRequests = (JSONArray) serverRequestDataContent;
			for(int i = 0; i < serverRequests.size(); i++) {
				Object requestData = serverRequests.get(i);
				JSONObject singleRequest = (JSONObject) requestData;
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
			try (FileWriter file = new FileWriter("src/main/resources/Server/requests_Server_" + this.id + ".json")) {
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
		try (FileReader reader = new FileReader("src/main/resources/Server/requests_Server_" + this.id + ".json"))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject requestsData = (JSONObject) jsonContent;
			Object serverRequestDataContent = requestsData.get("ServerRequests");
			JSONArray serverRequests = (JSONArray) serverRequestDataContent;
			for(int i = 0; i < serverRequests.size(); i++) {
				Object requestData = serverRequests.get(i);
				JSONObject singleRequest = (JSONObject) requestData;
				if(singleRequest.get("BookingId").toString().equals(bookingId)) {
					serverRequests.remove(i);
					break;
				}
			}
			try (FileWriter file = new FileWriter("src/main/resources/Server/requests_Server_" + this.id + ".json")) {
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
