package HotelService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import Message.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HotelBroker implements Runnable {

	//Attribute
	private static final Logger logger = LogManager.getRootLogger();
    private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer;
    private Hotel hotel;
    private String brokerName;
    
    int hotelBrokerPort;
    
    public HotelBroker(String brokerName, int hotelBrokerPort) {
    	logger.trace("Creating HotelBroker...");
    	this.brokerName = brokerName;
    	this.hotel = new Hotel(brokerName);
    	this.hotelBrokerPort = hotelBrokerPort;
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
				case 0:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(0, InetAddress.getLocalHost(), socket.getLocalPort(), 0, "InitialMessageResponseHotelBroker");
					}
					break;
				case 1:
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
					break;
				case 8:
					if(statusMessage.equals("HiFromServerMessageHandler")) {
						response = new Message(8, InetAddress.getLocalHost(), socket.getLocalPort(), 0, "OK");
					}
					break;
				default:
					response = new Message(-1, InetAddress.getLocalHost(), socket.getLocalPort(), 9, "ERROR ID_FormatException");
					break;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return response;
	}
}