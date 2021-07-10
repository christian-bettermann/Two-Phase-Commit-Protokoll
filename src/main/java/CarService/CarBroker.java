package CarService;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import Message.*;
import Request.CarRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CarBroker implements Runnable {

	//Attribute
	private static final Logger logger = LogManager.getRootLogger();
	private final MessageFactory msgFactory;
	private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer;
    private int carBrokerPort;
    private final CarPool pool;
    private String brokerName;
    private boolean wasAbortBefore;
    private InetAddress localAddress;
    
    public CarBroker() {
    	logger.trace("Creating CarBroker...");
    	this.msgFactory = new MessageFactory();
		this.pool = new CarPool();
		this.pool.initialize();
		this.wasAbortBefore = false;
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
					response = msgFactory.buildInfoCars(msg.getBookingID(), pool.getInfoOfCars(), localAddress, carBrokerPort);
					DatagramPacket packetCar = new DatagramPacket(response.toString().getBytes(), response.toString().getBytes().length, msg.getSenderAddress(), msg.getSenderPort());
					logger.trace("<CarBroker> sent: <"+ new String(packetCar.getData(), 0, packetCar.getLength()) +">");
					socket.send(packetCar);
					response = null;
					break;
				case PREPARE:
					if(this.pool.checkCarOfId(msg.getSenderAddress(), msg.getSenderPort(), msg.getBookingID(),Integer.parseInt(msg.getStatusMessageCarId()),new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()))) {
						response = msgFactory.buildReady(msg.getBookingID(), "CarIsFree", localAddress, carBrokerPort);
					} else {
						response = msgFactory.buildAbort(msg.getBookingID(), "CarIsAlreadyBlocked", localAddress, carBrokerPort);
						this.wasAbortBefore = true;
					}
					break;
				case COMMIT:
					this.pool.commitRequestOfBookingID(msg.getBookingID());
					response = msgFactory.buildAcknowledge(msg.getBookingID(), "ReservationHasBeenBooked", localAddress, carBrokerPort);
					break;
				case ROLLBACK:
					if(!this.wasAbortBefore) {
						this.pool.roolbackRequestOfBookingID(msg.getBookingID());
					} else {
						this.wasAbortBefore = false;
						this.pool.removeRequestFromList(msg.getBookingID());
					}
					response = msgFactory.buildAcknowledge(msg.getBookingID(), "ReservationHasBeenDeleted", localAddress, carBrokerPort);
					break;
				case TESTING:
					if(statusMessage.equals("HiFromServerMessageHandler")) {
						response = msgFactory.buildTest(msg.getBookingID(), "OK", localAddress, carBrokerPort);
					}
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = msgFactory.buildConnectionTest(msg.getBookingID(), "HiFromCarBroker", localAddress, carBrokerPort);
					}
					break;
				case INQUIRE:
					break;
				case THROWAWAY:
					break;
				default:
					response = msgFactory.buildError(null,  "ERROR ID_FormatException", localAddress, carBrokerPort);
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
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		logger.info("Starting " + brokerName + " on port <" + carBrokerPort + "> ...");
		try {
			socket = new DatagramSocket(carBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		ArrayList<CarRequest> oldRequests = pool.getRequests();
		DatagramPacket packet;
		CarRequest singleOldRequest;
		if(oldRequests.size() > 0) {
			for(int i = 0; i < oldRequests.size(); i++) {
				singleOldRequest = oldRequests.get(i);
				Message msg = msgFactory.buildInquire(singleOldRequest.getId(), "PlsSendAgain", localAddress, carBrokerPort);
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
		return carBrokerPort;
	}
	
	public void closeSocket() {
		socket.close();
	}
}