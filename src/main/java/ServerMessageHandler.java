import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
	protected MessageFactory msgFactory;										//message builder
	private DecisionHandler decisionHandler;									//handles response for messages
	private final String requestFilePath;										//json file path to store open requests 
	private final JsonHandler jsonHandler;										//works on json files (stable store)
	private static final Logger logger = LogManager.getRootLogger();			//shared logger
	protected final DatagramSocket socket;										//UDP socket
	private final BlockingQueue<Message> incomingMessages;						//shared queue with messages that the server received
	protected final String name;												//ServerMessageHandler name
	private Boolean online;														//keeps the while loop alive
	protected final Server server;												//server, that started this ServerMessageHandler
	private final ArrayList<ServerRequest> requestList;							//local object of open requests
	private Semaphore sem;														//Semaphore to sync file access
	Thread smhtcThread;															//ServerMessageHandlerTimeChecker Thread
	
	/**
	 * A constructor to create a new ServerMessageHandler
	 * @param	id: 	the id of the server, so it knows which request file to use
	 * 			name:	the name of the server, so it writes correct logs
	 * 			incomingMessages:	the shared queue for the messages received by the server
	 * 			socket:	the shared UDP socket of the server
	 * 			server:	the server, that started this ServerMessageHandler
	 */
	public ServerMessageHandler(int id, String name, BlockingQueue<Message> incomingMessages, DatagramSocket socket, Server server) {
		this.msgFactory = new MessageFactory();
		this.decisionHandler = new DecisionHandler(this, this.msgFactory);
		this.jsonHandler = new JsonHandler();
		this.requestFilePath = "src/main/resources/Server/requests_Server_" + id + ".json";
		this.incomingMessages = incomingMessages;
		this.name = name;
		this.socket = socket;
		this.server = server;
		this.requestList = new ArrayList<>();
		this.sem = new Semaphore(1, true);
		this.initialize();
	}
	
	/**
	 * A function to start the ServerMessageHandler
	 */
	public void run() {
		logger.info("Starting <" + name + "> for port <" + socket.getLocalPort() + ">...");
		online = true;
		ServerMessageHandlerTimeChecker smhtc = new ServerMessageHandlerTimeChecker(this);
		smhtcThread = new Thread(smhtc);
		smhtcThread.start();
		while (online) {
        	try {
        		if(incomingMessages.size() > 0) {
					Message inMsg = incomingMessages.take();
					logger.info("<" + name + "> removed Message from Queue:	<"+ inMsg.toString() +">");
					Message outMsg = this.analyzeAndGetResponse(inMsg);
					if(outMsg != null) {
						DatagramPacket packet = new DatagramPacket(outMsg.toString().getBytes(), outMsg.toString().getBytes().length, inMsg.getSenderAddress(), inMsg.getSenderPort());
						logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
						socket.send(packet);
					}
        		}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
        socket.close();
	}
	
	/**
	 * A method to handle the different message types
	 * @param	msg: the message that should be handled
	 * @return 	response:	the response for the handled message
	 */
	private Message analyzeAndGetResponse(Message msg) {
		String statusMessage = msg.getStatusMessage();
		Message response = null;
		ServerRequest request = this.getRequest(msg.getBookingID());
		try {
			switch(msg.getStatus()) {
				case INFO:
					//pass the message for the dynamic load of the car and room data to the brokers
					logger.trace("Handling Info message");
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
						//send the assigned bookingId for the request to the client
						response = msgFactory.buildBooking(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						
						//send the PREPAREs to the brokers
						Message prepareMsgCar = msgFactory.buildPrepare(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						answerParticipant(prepareMsgCar, server.getCarBroker().getAddress(),server.getCarBroker().getPort());
						Message prepareMsgHotel = msgFactory.buildPrepare(newBookingID, msg.getStatusMessage(), InetAddress.getLocalHost(), socket.getLocalPort());
						answerParticipant(prepareMsgHotel, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
						//add request to local object and stable store
						this.addRequestToList(newBookingID, Integer.parseInt(msg.getStatusMessageCarId()), Integer.parseInt(msg.getStatusMessageRoomId()), new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()), msg.getSenderAddress(), msg.getSenderPort(), new Date());
					} else {
						response = msgFactory.buildError(null, "ERROR_Invalid_Booking", InetAddress.getLocalHost(), socket.getLocalPort());
					}
					break;
				case READY:
					response = decisionHandler.handleReady(msg, request);
					TimeUnit.SECONDS.sleep(5);
					break;
				case ABORT:
					response = decisionHandler.handleAbort(msg, request);
					break;
				case INQUIRE:
					response = decisionHandler.handleInquire(msg, request);
					break;
				case ACKNOWLEDGMENT:
					response = decisionHandler.handleAcknowledge(msg, request);
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

	/**
	 * A method to add a new request to the local object and the request file
	 * @param	bookingId: 	the bookingId of the request
	 * 			carId:		the id of the requested car
	 * 			roomId:		the id of the requested room
	 * 			startTime:	the startTime of the request
	 * 			endTime:	the endTime of the request
	 * 			clientAddress:	the ip of the client that send the request
	 * 			clientPort:	the port of the client that send the request
	 * 			timestamp:	the time the request was received by the server
	 */
	private void addRequestToList(String bookingId, int carId, int roomId, Date startTime, Date endTime, InetAddress clientAddress, int clientPort, Date timestamp) {
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		this.requestList.add(new ServerRequest(bookingId, carId, roomId, startTime, endTime, clientAddress, clientPort, StatusTypes.INITIALIZED, StatusTypes.INITIALIZED, timestamp, StatusTypes.INITIALIZED));
		//add to stable store
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
			serverRequest.put("Timestamp", timestamp.getTime());
			serverRequest.put("ClientAddress", clientAddress.getHostAddress());
			serverRequest.put("ClientPort", clientPort);
			serverRequest.put("CarState", StatusTypes.INITIALIZED.toString());
			serverRequest.put("HotelState", StatusTypes.INITIALIZED.toString());
			serverRequest.put("GlobalState", StatusTypes.INITIALIZED.toString());
			serverRequests.add(serverRequest);
			try (FileWriter file = new FileWriter(this.requestFilePath)) {
				file.write(requestsData.toJSONString());
				file.flush();
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		sem.release();
	}

	/**
	 * A method to add a new request to the local object and the request file
	 * @param	bookingId: 	the bookingId of the request that should be updated
	 * 			newTimestamp:	the new timestamp for the request
	 */
	protected void updateRequestTimestamp(String bookingId, Date newTimestamp) {
		if(this.getRequest(bookingId) == null) {
			return;
		}
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		ServerRequest request = getRequest(bookingId);
		boolean updated = false;
		if(request.getTimestamp().before(newTimestamp)) {
			request.setTimestamp(newTimestamp);
		}
		//update timestamp in stable store
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(this.requestFilePath)) {
			JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
			JSONArray serverRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("ServerRequests"));
			for(int i = 0; i < serverRequests.size(); i++) {
				JSONObject singleRequest = jsonHandler.getAttributeAsJsonObject(serverRequests.get(i));
				if(singleRequest.get("BookingId").toString().equals(bookingId)) {
					if(new Date(Long.parseLong(singleRequest.get("Timestamp").toString())).before(newTimestamp)) {
						singleRequest.replace("Timestamp", newTimestamp.getTime());
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
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		sem.release();
	}
	
	/**
	 * A method to update information about a booking request in the local object and the request file
	 * @param	bookingId: 	the bookingId of the request
	 * 			carState:	the new state, that the carBroker sent
	 * 			hotelId:	the new state, that the hotelBroker sent
	 * 			globalState:	the new state, that the server calculated
	 */
	protected void updateRequestAtList(String bookingId, StatusTypes carState, StatusTypes hotelState, StatusTypes globalState) {
		if(getRequest(bookingId) == null) {
			return;
		}
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		ServerRequest request = getRequest(bookingId);
		boolean updated = false;
		if(!(request.getCarBrokerState().equals(carState)) && carState != null) {
			request.setCarBrokerState(carState);
		}
		if(!(request.getHotelBrokerState().equals(hotelState)) && hotelState != null) {
			request.setHotelBrokerState(hotelState);
		}
		//pass the decision to the client if the globalState (server decision) changes
		if(!(request.getGlobalState().equals(globalState)) && globalState != null) {
			request.setGlobalState(globalState);
			try {
				if(globalState.equals(StatusTypes.COMMIT)) {
					Message commitClient = msgFactory.buildCommit(request.getId(), request.contentToString(), InetAddress.getLocalHost(), socket.getLocalPort());
					answerParticipant(commitClient, request.getClientAddress(), request.getClientPort());
				}
				if(globalState.equals(StatusTypes.ROLLBACK)) {
					Message rollbackClient = msgFactory.buildRollback(request.getId(), request.getCarBrokerState() + "_" + request.getHotelBrokerState(), InetAddress.getLocalHost(), socket.getLocalPort());
					answerParticipant(rollbackClient, request.getClientAddress(), request.getClientPort());
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		//update stable store
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
					if (globalState != null && !StatusTypes.valueOf(singleRequest.get("GlobalState").toString()).equals(globalState)) {
						singleRequest.replace("GlobalState", globalState.toString());
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
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		sem.release();
	}

	/**
	 * A method to remove a booking request from the local object and the request file
	 * @param	bookingId: 	the bookingId of the request
	 * 			carState:	the new state, that the carBroker sent
	 * 			hotelId:	the new state, that the hotelBroker sent
	 * 			globalState:	the new state, that the server calculated
	 */
	protected void removeRequestFromList(String bookingId) {
		for(int i = 0; i < requestList.size(); i++) {
			if(this.requestList.get(i).getId().equals(bookingId)) {
				this.requestList.remove(i);
				break;
			}
		}
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		//remove from stable store
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(this.requestFilePath)) {
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
		sem.release();
	}

	//A function to read data from the stable store and build local objects from the data
	private void initialize() {
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		//read open request from stable store
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader(requestFilePath))
		{
			JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
			JSONArray requests = jsonHandler.getAttributeAsJsonArray(requestsData.get("ServerRequests"));
			for (int i = 0; i < requests.size(); i++) {
				JSONObject requestInfo = jsonHandler.getAttributeAsJsonObject(requests.get(i));
				ServerRequest singleServerRequest = new ServerRequest(requestInfo.get("BookingId").toString(),
					Integer.parseInt(requestInfo.get("CarId").toString()),
					Integer.parseInt(requestInfo.get("RoomId").toString()),
					new Date(Long.parseLong(requestInfo.get("StartTime").toString())),
					new Date(Long.parseLong(requestInfo.get("EndTime").toString())),
					InetAddress.getByName(requestInfo.get("ClientAddress").toString()),
					Integer.parseInt(requestInfo.get("ClientPort").toString()),
					StatusTypes.valueOf(requestInfo.get("CarState").toString()),
					StatusTypes.valueOf(requestInfo.get("HotelState").toString()),
					new Date(Long.parseLong(requestInfo.get("Timestamp").toString())),
					StatusTypes.valueOf(requestInfo.get("GlobalState").toString())
				);
				//add the open request to the local object
				this.requestList.add(singleServerRequest);
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		ServerRequest singleOldRequest;
		//resent messages to proceed with the requests
		if(requestList.size() > 0) {
			for(int i = 0; i < requestList.size(); i++) {
				singleOldRequest = requestList.get(i);
				singleOldRequest.setTimestamp(new Date());
				//resent server decisions if an ACKNOWLEDGEMENT is missing
				if(singleOldRequest.getGlobalState() == StatusTypes.COMMIT) {
					if(!singleOldRequest.getCarBrokerState().equals(StatusTypes.ACKNOWLEDGMENT)) {
						Message msgForCarBroker = msgFactory.buildCommit(singleOldRequest.getId(), "OkThenCommit", this.socket.getLocalAddress(), this.socket.getLocalPort());
						answerParticipant(msgForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
					}
					if(!singleOldRequest.getHotelBrokerState().equals(StatusTypes.ACKNOWLEDGMENT)) {
						Message msgForHotelBroker = msgFactory.buildCommit(singleOldRequest.getId(), "OkThenCommit", this.socket.getLocalAddress(), this.socket.getLocalPort());
						answerParticipant(msgForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
					}
				} else if(singleOldRequest.getGlobalState() == StatusTypes.ROLLBACK) {
					if(!singleOldRequest.getCarBrokerState().equals(StatusTypes.ACKNOWLEDGMENT)) {
						Message msgForCarBroker = msgFactory.buildRollback(singleOldRequest.getId(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						answerParticipant(msgForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
					}
					if(!singleOldRequest.getHotelBrokerState().equals(StatusTypes.ACKNOWLEDGMENT)) {
						Message msgForHotelBroker = msgFactory.buildRollback(singleOldRequest.getId(), "OkThenRollback", this.socket.getLocalAddress(), this.socket.getLocalPort());
						answerParticipant(msgForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
					}
				} else {
					Message msgForCarBroker;
					Message msgForHotelBroker;
					//resent PREPAREs if a broker decision is missing
					switch(singleOldRequest.getCarBrokerState()) {
						case INITIALIZED:
							msgForCarBroker = msgFactory.buildPrepare(singleOldRequest.getId(), singleOldRequest.contentToString(), this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(msgForCarBroker, server.getCarBroker().getAddress(), server.getCarBroker().getPort());
							break;
						default:
							break;
					}
					switch(singleOldRequest.getHotelBrokerState()) {
						case INITIALIZED:
							msgForHotelBroker = msgFactory.buildPrepare(singleOldRequest.getId(), singleOldRequest.contentToString(), this.socket.getLocalAddress(), this.socket.getLocalPort());
							answerParticipant(msgForHotelBroker, server.getHotelBroker().getAddress(), server.getHotelBroker().getPort());
							break;
						default:
							break;
					}
				}
			}
		}
		sem.release();
	}

	/**
	 * A method to get a request from the local object
	 * @param	bookingId: 	the bookingId of the request
	 * @return	request:	the wanted request
	 */
	protected ServerRequest getRequest(String bookingId) {
		ServerRequest request = null;
		//go through the local object and check if the bookingIds match
		for(int i = 0; i < requestList.size(); i++) {
			if(this.requestList.get(i).getId().equals(bookingId)) {
				request = this.requestList.get(i);
				break;
			}
		}
		return request;
	}

	protected ArrayList<ServerRequest> getRequestList() {
		return requestList;
	}

	/**
	 * A method to send a message to a participant
	 * @param	msg: 	the message you want to send
	 * 			participantAddress:		address of the participant you want to send the message to
	 * 			participantPort:		port of the participant you want to send the message to
	 */
	protected void answerParticipant(Message msg, InetAddress participantAddress, int participantPort) {
		DatagramPacket dp = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, participantAddress, participantPort);
		logger.trace("<" + name + "> sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
		try {
			socket.send(dp);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A method to check if an address and port matches with the data of the carBroker
	 * @param	pAddress: 	the address you want to check
	 * 			pPort:		the port you want to check
	 * @return	result:		true if it matches with the carBroker data, otherwise false
	 */
	private boolean messageFromCarBroker(InetAddress pAddress, int pPort) {
		boolean result = false;
		if(pAddress.equals(server.getCarBroker().getAddress()) && pPort == server.getCarBroker().getPort()) {
			result = true;
		}
		return result;
	}

	/**
	 * A method to check if an address and port matches with the data of the hotelBroker
	 * @param	pAddress: 	the address you want to check
	 * 			pPort:		the port you want to check
	 * @return	result:		true if it matches with the hotelBroker data, otherwise false
	 */
	private boolean messageFromHotelBroker(InetAddress pAddress, int pPort) {
		boolean result = false;
		if(pAddress.equals(server.getHotelBroker().getAddress()) && pPort == server.getHotelBroker().getPort()) {
			result = true;
		}
		return result;
	}
	
	public void shutdownServerMessageTimeHandler() {
		smhtcThread.stop();
	}

	public Server getServer() {
		return this.server;
	}

	public InetAddress getLocalAddress() {
		return this.socket.getLocalAddress();
	}

	public int getLocalPort() {
		return this.socket.getLocalPort();
	}
}