package HotelService;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import Message.*;
import Request.RoomRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class HotelBroker implements Runnable {

	//Attribute
	private static final Logger logger = LogManager.getRootLogger();
	private final MessageFactory msgFactory;
    private static DatagramSocket socket;
    private boolean online;
	private byte[] buffer;
    private final Hotel hotel;
    private String brokerName;
    private InetAddress localAddress;
    int hotelBrokerPort;
    
    public HotelBroker() {
    	logger.trace("Creating HotelBroker...");
    	this.msgFactory = new MessageFactory();
    	this.hotel = new Hotel();
		this.hotel.initialize();
		this.initialize();
    }

	public void run() {
        online = true;
        while (online) {
        	try {
				buffer = new byte[1024];
        		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
				InetAddress address = dp.getAddress();
				int port = dp.getPort();
	            Message received = new Message(new String(dp.getData(), 0, dp.getLength()));
	            logger.info(brokerName + " received: <"+ received.toString() +">");
				Message response = this.analyzeAndGetResponse(received);
				if(response != null) {
					buffer = response.toString().getBytes();
					dp = new DatagramPacket(buffer, buffer.length, address, port);
					logger.trace(brokerName + " sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
		            socket.send(dp);
				}
        	} catch (IOException e) {
				e.printStackTrace();
			}
        }
        socket.close();
	}

	/**
	 * A method to get an answer for an incoming network package
	 * @param msg Message to find an answer for
	 * @return    It returns the requested answer for the request
	 *
	 */
	private Message analyzeAndGetResponse(Message msg) {
		String statusMessage = msg.getStatusMessage();
		Message response = null;
		try {
			switch(msg.getStatus()) {
				case INFO:
					//answer with a list oft all rooms
					response = msgFactory.buildInfoRooms(msg.getBookingID(), hotel.getInfoOfRooms(), localAddress, hotelBrokerPort);
					DatagramPacket packetHotel = new DatagramPacket(response.toString().getBytes(), response.toString().getBytes().length, msg.getSenderAddress(), msg.getSenderPort());
					logger.trace("<HotelBroker> sent: <"+ new String(packetHotel.getData(), 0, packetHotel.getLength()) +">");
					socket.send(packetHotel);
					response = null;
					break;
				case PREPARE:
					if(this.hotel.checkRoomOfId(msg.getSenderAddress(), msg.getSenderPort(), msg.getBookingID(), Integer.parseInt(msg.getStatusMessageRoomId()),new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()))) {
						response = msgFactory.buildReady(msg.getBookingID(), "HotelRoomIsFree", localAddress, hotelBrokerPort);
					} else {
						response = msgFactory.buildAbort(msg.getBookingID(), "HotelRoomIsAlreadyBlocked", localAddress, hotelBrokerPort);
					}
					break;
				case COMMIT:
					this.hotel.commitRequestOfBookingID(msg.getBookingID());
					response = msgFactory.buildAcknowledge(msg.getBookingID(),"ReservationHasBeenBooked", localAddress, hotelBrokerPort);
					logger.error("################################# Press Shutdown quickly COMMIT #################################");
					TimeUnit.SECONDS.sleep(5);
					break;
				case ROLLBACK:
					this.hotel.rollbackRequestOfBookingID(msg.getBookingID());
					response = msgFactory.buildAcknowledge(msg.getBookingID(), "ReservationHasBeenDeleted", localAddress, hotelBrokerPort);
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = msgFactory.buildConnectionTest(msg.getBookingID(), "HiFromHotel", localAddress, hotelBrokerPort);
					}
					break;
				case INQUIRE:
					if(hotel.inquireMessage(msg.getBookingID())) {
						response = msgFactory.buildAcknowledge(msg.getBookingID(), "ReservationHasBeenBooked", localAddress, hotelBrokerPort);
					} else {
						response = msgFactory.buildAcknowledge(msg.getBookingID(), "ReservationHasBeenDeleted", localAddress, hotelBrokerPort);
					}
					break;
				case THROWAWAY:
					this.hotel.undoEverything(msg.getBookingID());
					break;
				default:
					response = msgFactory.buildError(null, "ERROR ID_FormatException", localAddress, hotelBrokerPort);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private void initialize() {
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader("src/main/resources/HotelService/config.json"))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject configData = (JSONObject) jsonContent;
			this.brokerName = configData.get("serviceName").toString();
			this.localAddress = InetAddress.getByName(configData.get("ip").toString());
			this.hotelBrokerPort = Integer.parseInt(configData.get("port").toString());
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		logger.info("Starting " + brokerName + " on port <" + hotelBrokerPort + "> ...");
		try {
			socket = new DatagramSocket(hotelBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		ArrayList<RoomRequest> oldRequests = hotel.getRequests();
		DatagramPacket packet;
		RoomRequest singleOldRequest;
		if(oldRequests.size() > 0) {
			for(int i = 0; i < oldRequests.size(); i++) {
				singleOldRequest = oldRequests.get(i);
				Message msg = msgFactory.buildInquire(singleOldRequest.getId(), "PlsSendAgain", localAddress, hotelBrokerPort);
				packet = new DatagramPacket(msg.getAsBytes(), msg.getAsBytes().length, singleOldRequest.getTargetIp(), singleOldRequest.getTargetPort());
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public InetAddress getLocalAddress() {
		return localAddress;
	}
	
	public int getPort() {
		return hotelBrokerPort;
	}
	
	public void closeSocket() {
		socket.close();
	}
}