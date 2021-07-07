package CarService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

import Message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CarBroker implements Runnable {

	//Attribute
	private static final Logger logger = LogManager.getRootLogger();
	private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer = new byte[1024];
    private int carBrokerPort;
    private CarPool pool;
    private String brokerName;
    private InetAddress localAddress;
    
    public CarBroker() {
    	this.initialize();
    	logger.trace("Creating CarBroker...");
		this.pool = new CarPool(this.brokerName);
		this.pool.initialize();
    }
    
    public void run() {
    	logger.info("Starting CarBroker on port <" + carBrokerPort + "> ...");
    	try {
			socket = new DatagramSocket(carBrokerPort);
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
	            logger.info("CarBroker received: <"+ received.toString() +">");
				Message response = this.analyzeAndGetResponse(received);
				if(response != null) {
					buffer = response.toString().getBytes();
					dp = new DatagramPacket(buffer, buffer.length, address, port);
					logger.trace("CarBroker sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
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
					//answer with a list oft all cars
					//#####################################
					response = new Message(StatusTypes.INFOCARS, localAddress, socket.getLocalPort(), msg.getBookingID(), pool.getInfoOfCars());;
					break;
				case PREPARE:
					if(this.pool.checkCarOfId(msg.getSenderAddress(), msg.getSenderPort(), Integer.parseInt(msg.getBookingID()),Integer.parseInt(msg.getStatusMessageCarId()),new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()))) {
						response = new Message(StatusTypes.READY, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
						//write to stable store
						//############################
					} else {
						response = new Message(StatusTypes.ABORT, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
						//write to stable store
						//############################
					}
					break;
				case COMMIT:
					//proceed with booking of car
					//write to stable store
					this.pool.commitRequestOfBookingID(Integer.parseInt(msg.getBookingID()));
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
					break;
				case ROLLBACK:
					//cancel booking of car
					//write to stable store
					//############################
					this.pool.roolbackRequestOfBookingID(Integer.parseInt(msg.getBookingID()));
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, localAddress, socket.getLocalPort(), msg.getBookingID(), msg.getStatusMessage());
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
						response = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), "0", "InitialMessageResponseCarBroker");
					}
					break;
				case INQUIRE:
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
		try (FileReader reader = new FileReader("src/main/resources/CarService/config.json"))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject configData = (JSONObject) jsonContent;
			this.brokerName = configData.get("serviceName").toString();
			this.localAddress = InetAddress.getByName(configData.get("ip").toString());
			this.carBrokerPort = Integer.parseInt(configData.get("port").toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			socket = new DatagramSocket(carBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public InetAddress getLocalAddress() {
		return localAddress;
	}
	
	public int getPort() {
		return carBrokerPort;
	}
	
	public void closeSocket() {
		socket.close();
	}
}