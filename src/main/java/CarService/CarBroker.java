package CarService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarBroker {

	//Attribute
	private static final Logger logger = LogManager.getRootLogger();
	private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer = new byte[1024];
    private int carBrokerPort;
    private CarPool pool;
    
    public CarBroker(int carBrokerPort) {
    	logger.info("Creating CarBroker...");
		this.pool = new CarPool("Sixt");
    	this.carBrokerPort = carBrokerPort;
    }
    
    public void start() {
    	logger.info("Starting CarBroker...");
    	try {
			socket = new DatagramSocket(carBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
        online = true;
        
        while (online) {
        	try {
        		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
	            InetAddress address = dp.getAddress();
	            int port = dp.getPort();
	            String received = new String(dp.getData(), 0, dp.getLength());
				String response = this.analyzeAnGetResponse(received);
				buffer = response.getBytes();
				dp = new DatagramPacket(buffer, buffer.length, address, port);
	            socket.send(dp);
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
	private String analyzeAnGetResponse(String msg) {
		String response = "";
		int id = extractMessageType(msg);
		String contentOfMessage = extractMessageContent(msg);
		switch(id) {
			case 0:
				if(contentOfMessage.equals("InitialMessageRequest")) {
					response = "0" + " " +  "InitialMessageResponseCarBroker";
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
			default:
				response = "-1" + " " + "ERROR ID_FormatException";
				break;
		}
    	return response;
	}

	/**
	 * A method to extract the MessageId from an incoming network package
	 * @param msg Message to extract the id from
	 * @return    Normally it returns the id from 0 to 5
	 *            In case of bad format it return -1
	 */
	private int extractMessageType(String msg) {
    	int typeId;
    	String[] saveArray = msg.split(" ");

    	try {
			typeId = Integer.parseInt(saveArray[0].trim());
		} catch(Exception e) {
    		typeId = -1;
		}
    	return typeId;
	}

	/**
	 * A method to extract the content from an incoming network package
	 * @param msg Message to extract the content from
	 * @return    It returns the content from the message
	 *
	 */
	private String extractMessageContent(String msg) {
		String typeContent;
		try {
			String[] saveArray = msg.split(" ");
			typeContent = saveArray[1].trim();
		} catch (Exception e) {
			typeContent = " ";
		}
		return typeContent;
	}
}