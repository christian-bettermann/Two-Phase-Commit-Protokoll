package CarService;

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
import Request.CarRequest;
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
    private boolean wasAbortBefore;
    private InetAddress localAddress;
    
    public CarBroker() {
    	logger.trace("Creating CarBroker...");
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
	            Message received = new Message(new String(dp.getData(), 0, dp.getLength()), address, port);
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
					Message res = new Message(StatusTypes.INFOCARS, this.localAddress, this.carBrokerPort, msg.getBookingID(), pool.getInfoOfCars());
					DatagramPacket packetCar = new DatagramPacket(res.toString().getBytes(), res.toString().getBytes().length, msg.getSenderAddress(), msg.getSenderPort());
					logger.trace("<CarBroker> sent: <"+ new String(packetCar.getData(), 0, packetCar.getLength()) +">");
					socket.send(packetCar);
					response = null;
					break;
				case PREPARE:
					if(this.pool.checkCarOfId(msg.getSenderAddress(), msg.getSenderPort(), msg.getBookingID(),Integer.parseInt(msg.getStatusMessageCarId()),new Date(msg.getStatusMessageStartTime()), new Date(msg.getStatusMessageEndTime()))) {
						response = new Message(StatusTypes.READY, this.localAddress, this.carBrokerPort, msg.getBookingID(), "CarIsFree");
						//write to stable store
						//############################
					} else {
						response = new Message(StatusTypes.ABORT, this.localAddress, this.carBrokerPort, msg.getBookingID(), "CarIsAlreadyBlocked");
						this.wasAbortBefore = true;
						//write to stable store
						//############################
					}
					break;
				case COMMIT:
					//proceed with booking of car
					//write to stable store
					this.pool.commitRequestOfBookingID(msg.getBookingID());
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, this.localAddress, this.carBrokerPort, msg.getBookingID(), "ReservationHasBeenBooked");
					break;
				case ROLLBACK:
					//cancel booking of car
					//write to stable store
					//############################
					if(!this.wasAbortBefore) {
						this.pool.roolbackRequestOfBookingID(msg.getBookingID());
					} else {
						this.wasAbortBefore = false;
						this.pool.removeRequestFromList(msg.getBookingID());
					}
					//sending ACKNOWLEDGMENT to server
					response = new Message(StatusTypes.ACKNOWLEDGMENT, this.localAddress, this.carBrokerPort, msg.getBookingID(), "ReservationHasBeenDeleted");
					break;
				case TESTING:
					if(statusMessage.equals("HiFromServerMessageHandler")) {
						response = new Message(StatusTypes.TESTING, this.localAddress, this.carBrokerPort, msg.getBookingID(), "OK");
					}
					break;
				case ERROR:
					break;
				case CONNECTIONTEST:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(StatusTypes.CONNECTIONTEST, this.localAddress, this.carBrokerPort, msg.getBookingID(), "HiFromCarBroker");
					}
					break;
				case INQUIRE:
					break;
				default:
					response = new Message(StatusTypes.ERROR, this.localAddress, this.carBrokerPort, null, "ERROR ID_FormatException");
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
		logger.info("Starting CarBroker on port <" + carBrokerPort + "> ...");
		try {
			socket = new DatagramSocket(carBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		ArrayList<CarRequest> oldRequests = pool.getRequests();
		DatagramPacket packet;
		byte[] dataBytes;
		if(oldRequests.size() > 0) {
			for(int i = 0; i < oldRequests.size(); i++) {
				CarRequest singleOldRequest = oldRequests.get(i);
				Message msg = new Message(singleOldRequest.getState(), singleOldRequest.getTargetIp(), singleOldRequest.getTargetPort(), singleOldRequest.getId(), "");
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
		return carBrokerPort;
	}
	
	public void closeSocket() {
		socket.close();
	}
}