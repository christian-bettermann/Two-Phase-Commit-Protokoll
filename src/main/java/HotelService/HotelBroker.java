package HotelService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;

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
    private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer;
    private Hotel hotel;
    private String brokerName;
    private InetAddress localAddress;
    int hotelBrokerPort;
    
    public HotelBroker() {
    	logger.trace("Creating HotelBroker...");
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
	            Message received = new Message(new String(dp.getData(), 0, dp.getLength()), address, port);
	            logger.info("HotelBroker received: <"+ received.toString() +">");
				Message response = this.analyzeAndGetResponse(received);
				if(response != null) {
					buffer = response.toString().getBytes();
					dp = new DatagramPacket(buffer, buffer.length, address, port);
					logger.trace("HotelBroker sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
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
		Message response = new Message();
		try {
			switch(msg.getStatus()) {
				case INFO:
					//answer with a list oft all rooms
					Message res= new Message(StatusTypes.INFOROOMS, this.localAddress, this.hotelBrokerPort,  msg.getBookingID(), hotel.getInfoOfRooms());
					DatagramPacket packetHotel = new DatagramPacket(res.toString().getBytes(), res.toString().getBytes().length, msg.getSenderAddress(), msg.getSenderPort());
					logger.trace("<HotelBroker> sent: <"+ new String(packetHotel.getData(), 0, packetHotel.getLength()) +">");
					socket.send(packetHotel);
					response = null;
					break;
				case PREPARE:
					if(this.hotel.checkRoomOfId(msg.getSenderAddress(), msg.getSenderPort(), msg.getBookingID(), Integer.parseInt(msg.getStatusMessageHotelId()),new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()))) {
						response = new Message(StatusTypes.READY, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "HotelRoomIsFree");

						//write to stable store
						//#################################
					} else {
						response = new Message(StatusTypes.ABORT, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "HotelRoomIsAlreadyBlocked");
						//write to stable store
						//#################################
					}
					break;
				case COMMIT:
					//proceed with booking of room
					//write to stable store
					this.hotel.commitRequestOfBookingID(msg.getBookingID());
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "ReservationHasBeenBooked");
					break;
				case ROLLBACK:
					//cancel booking of room
					//write to stable store
					//#################################
					this.hotel.roolbackRequestOfBookingID(msg.getBookingID());
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "ReservationHasBeenDeleted");
					break;
				case TESTING:
					if(statusMessage.equals("HiFromServerMessageHandler")) {
						response = new Message(StatusTypes.TESTING, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "OK");
					}
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(StatusTypes.CONNECTIONTEST, this.localAddress, this.hotelBrokerPort, msg.getBookingID(), "HiFromHotel");
					}
					break;	
				default:
					response = new Message(StatusTypes.ERROR, this.localAddress, this.hotelBrokerPort, null, "ERROR ID_FormatException");
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		logger.info("Starting HotelBroker on port <" + hotelBrokerPort + "> ...");
		try {
			socket = new DatagramSocket(hotelBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		ArrayList<RoomRequest> oldRequests = hotel.getRequests();
		DatagramPacket packet;
		byte[] dataBytes;
		if(oldRequests.size() > 0) {
			for(int i = 0; i < oldRequests.size(); i++) {
				RoomRequest singleOldRequest = oldRequests.get(i);
				Message msg = new Message(singleOldRequest.getState(), singleOldRequest.getTargetIp(), singleOldRequest.getTargetPort(), singleOldRequest.getIdAsString(), "");
				dataBytes = msg.toString().getBytes();
				packet = new DatagramPacket(dataBytes, dataBytes.length, singleOldRequest.getTargetIp(), singleOldRequest.getTargetPort());
				try {
					this.socket.send(packet);
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