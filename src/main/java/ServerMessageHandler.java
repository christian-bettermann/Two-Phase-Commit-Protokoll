import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
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
				
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
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
					//send info request to brokers //Client address and port in statusMessage
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
					//general check if booking is a valid request
					if(msg.getStatusMessageEndTime() > msg.getStatusMessageStartTime()) {
						//assign new BookingID built from ServerName+timestamp+UUID so it's unique
						String uniqueID = UUID.randomUUID().toString();
						String timestamp = String.valueOf(new Date().getTime());
						String newBookingID = server.getName()+ "_" + timestamp + "_" + uniqueID;
						//send newBookingID to client
						response = new Message(StatusTypes.BOOKING, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						
						//stable store clientAddress and port with bookingID
						//###############################################
						
						//send booking to brokers
						Message prepareMsgCar = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, server.getBroker()[0].getAddress(), server.getBroker()[0].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) +">");
						socket.send(preparePacketCar);
						
						Message prepareMsgHotel = new Message(StatusTypes.PREPARE, InetAddress.getLocalHost(), socket.getLocalPort(), newBookingID, msg.getStatusMessage());
						DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, server.getBroker()[1].getAddress(), server.getBroker()[1].getPort());
						logger.trace("<" + name + "> sent: <"+ new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) +">");
						socket.send(preparePacketHotel);
					} else {
						response = new Message(StatusTypes.ERROR, InetAddress.getLocalHost(), socket.getLocalPort(), null, "ERROR_Invalid_Booking");
					}
					break;
				case READY:
					//test for carBroker
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						//stable store carBroker ready for BookingID
						//##############################################
						logger.error("CARBROKER MESSAGE READY!!!!!!!!!!!!!!!!!!!");
						//check if stable store already has the hotelBroker ready => if true start COMMIT, if false start ROLLBACK
						//##############################################
						response = null;
					}
					
					//test for hotelBroker
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						//stable store hotelBroker ready for BookingID
						//##############################################
						logger.error("HOTELBROKER MESSAGE READY!!!!!!!!!!!!!!!!!!!");
						//check if stable store already has the carBroker ready => if true start COMMIT, if false start ROLLBACK
						//##############################################
						response = null;
					}
					
					break;
				case ABORT:
					//test for carBroker
					logger.error(msg.getSenderAddress());
					logger.error(server.getBroker()[0].getAddress());
							logger.error(msg.getSenderPort());
									logger.error(server.getBroker()[0].getPort());
					if(msg.getSenderAddress().equals(server.getBroker()[0].getAddress()) && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						//stable store carBroker abort for BookingID
						//##############################################
						logger.error("CARBROKER MESSAGE ABORT!!!!!!!!!!!!!!!!!!!");
						//send rollback to all brokers => booking failed: send ERROR to client for bookingID
						//##############################################
					}
					
					//test for hotelBroker
					if(msg.getSenderAddress().equals(server.getBroker()[1].getAddress()) && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						//stable store hotelBroker abort for BookingID
						//##############################################
						logger.error("HOTELBROKER MESSAGE ABORT!!!!!!!!!!!!!!!!!!!");
						//send rollback to all brokers => booking failed: send ERROR to client for bookingID
						//##############################################
					}
					break;
				case ACKNOWLEDGMENT:
					//test for carBroker
					if(msg.getSenderAddress() == server.getBroker()[0].getAddress() && msg.getSenderPort() == server.getBroker()[0].getPort()) {
						//stable store carBroker ACKNOWLEDGMENT for BookingID
						//##############################################
						
						//check if stable store already has the hotelBroker ACKNOWLEDGMENT => booking finished: send ACKNOWLEDGMENT to client
						//##############################################
					}
					
					//test for hotelBroker
					if(msg.getSenderAddress() == server.getBroker()[1].getAddress() && msg.getSenderPort() == server.getBroker()[1].getPort()) {
						//stable store hotelBroker ACKNOWLEDGMENT for BookingID
						//##############################################
						
						//check if stable store already has the carBroker ACKNOWLEDGMENT => booking finished: send ACKNOWLEDGMENT to client
						//##############################################
					}
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
			serverRequest.put("State", "Initialized");
			serverRequests.add(serverRequest);
			try (FileWriter file = new FileWriter("src/main/resources/Server/requests_Server_" + this.id + ".json")) {
				file.write(requestsData.toJSONString());
				file.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private ServerRequest getRequest(int bookingId) {
		ServerRequest request = null;
		for(int i = 0; i < requestList.size(); i++) {
			if(this.requestList.get(i).getIdAsString().equals("" + bookingId + "")) {
				request = this.requestList.get(i);
				break;
			}
		}
		return request;
	}
}
