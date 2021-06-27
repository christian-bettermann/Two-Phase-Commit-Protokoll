package HotelService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import Message.*;
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
    	this.initialize();
    	logger.trace("Creating HotelBroker...");
    	this.hotel = new Hotel(brokerName);
    }
    
    public void run() {
    	logger.info("Starting HotelBroker on port <" + hotelBrokerPort + "> ...");
    	try {
			socket = new DatagramSocket(hotelBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
        online = true;
        
        
        while (online) {
        	try {
        		buffer = new byte[1024];
        		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
	            InetAddress address = dp.getAddress();
	            int port = dp.getPort();
	            Message received = new Message(new String(dp.getData(), 0, dp.getLength()));
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
					Message res= new Message(StatusTypes.INFOROOMS, localAddress, socket.getLocalPort(), "0", "###############################################ROOMS");
					DatagramPacket packetHotel = new DatagramPacket(res.toString().getBytes(), res.toString().getBytes().length, msg.getSenderAddress(), msg.getSenderPort());
					logger.trace("<HotelBroker> sent: <"+ new String(packetHotel.getData(), 0, packetHotel.getLength()) +">");
					socket.send(packetHotel);
					response = null;
					break;
				case PREPARE:
					//check if hotel is available with bookingMessage values
					//#########################################
					
					//hotel available
					response = new Message(StatusTypes.READY, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
					
					//hotel not available
					//response = new Message(StatusTypes.ABORT, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
					break;
				case COMMIT:
					//proceed with booking of room
					//write to stable store
					//############################
					
					//sending ACKNOWLEDGMENT to server
					//############################
					break;
				case ROLLBACK:
					//cancel booking of room
					//write to stable store
					//############################
					
					//sending ACKNOWLEDGMENT to server
					//############################
					break;
				case TESTING:
					if(statusMessage.equals("HiFromServerMessageHandler")) {
						response = new Message(StatusTypes.TESTING, localAddress, socket.getLocalPort(), "0", "OK");
					}
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), "0", "InitialMessageResponseHotelBroker");
					}
					break;	
				default:
					response = new Message(StatusTypes.ERROR, localAddress, socket.getLocalPort(), null, "ERROR ID_FormatException");
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private void initialize() {
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader("config.json"))
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
	}
}