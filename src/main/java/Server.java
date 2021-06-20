import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import Message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();
	private DatagramSocket socket;
	private byte[] buffer = new byte[1024];
	private String serverName;
	private boolean online = true;
	private BlockingQueue<Message> incomingMessages;
    private InetAddress carBrokerAddress;
    private InetAddress hotelBrokerAddress;
    private InetAddress localAddress;
    
	    
    int serverPort, carBrokerPort, hotelBrokerPort;
    
    boolean carBrokerOnline, hotelBrokerOnline;
    
	public Server (String serverName, int serverPort, InetAddress carBrokerAddress, int carBrokerPort, InetAddress hotelBrokerAddress, int hotelBrokerPort) {
		try {
			localAddress = InetAddress.getLocalHost();
			logger.error(localAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		incomingMessages = new ArrayBlockingQueue<Message>(1024);
		logger.trace("Creating Server <" + serverName + ">...");
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.carBrokerAddress = carBrokerAddress;
		this.hotelBrokerAddress = hotelBrokerAddress;
		this.carBrokerPort = carBrokerPort;
		this.hotelBrokerPort = hotelBrokerPort;
		carBrokerOnline = false;
		hotelBrokerOnline = false;
	}
	

	public void run() {
		logger.info("Starting Server <" + serverName + "> on port <" + serverPort + "> ...");
		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//Sending initial Messages to Brokers
		//initial CarBroker message
		do {
			checkCarBrokerAvailability();
			if(!carBrokerOnline) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (!carBrokerOnline);
		
		//initial HotelBroker message
		do {
			checkHotelBrokerAvailability();
			if(!hotelBrokerOnline) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (!hotelBrokerOnline);
		
		//Brokers are initialy available
		
		startMessageHandling();
	}
	
	public void checkCarBrokerAvailability() {
		try {
			logger.warn("!!!"+ localAddress);
			Message msg = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), 0, "InitialMessageRequest");
			logger.warn("!!!"+ msg.getSenderAddress());
			socket.setSoTimeout(2500);
			
		    DatagramPacket packet = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, carBrokerAddress, carBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buffer, buffer.length);
		    socket.receive(packet);
		    String premsg = new String(packet.getData(), 0, packet.getLength());
		    Message received = new Message(premsg);
		    logger.trace("Server received: "+ received.toString());
		    if(received.getStatus() == StatusTypes.CONNECTIONTEST && received.getStatusMessage().equals("InitialMessageResponseCarBroker")) {
		    	carBrokerOnline = true;
		    	logger.info("CarBroker is available for <" + serverName +">");
		    }
	    } catch (SocketTimeoutException e) {
            //Timeout
	    	logger.error("CarBroker is not available for <" + serverName +">!");
            carBrokerOnline = false;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void checkHotelBrokerAvailability() {
		try {
			Message msg = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), 0, "InitialMessageRequest");
			
			socket.setSoTimeout(2500);
			
		    DatagramPacket packet = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, hotelBrokerAddress, hotelBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buffer, buffer.length);
		    socket.receive(packet);
		    Message received = new Message(new String(packet.getData(), 0, packet.getLength()));
		    logger.trace("Server received: "+ received.toString());
		    if(received.getStatus() == StatusTypes.CONNECTIONTEST && received.getStatusMessage().equals("InitialMessageResponseHotelBroker")) {
		    	hotelBrokerOnline = true;
		    	logger.info("HotelBroker is available for <" + serverName +">");
		    }
	    } catch (SocketTimeoutException e) {
            //Timeout
	    	logger.error("HotelBroker is not available for <" + serverName +">!");
            hotelBrokerOnline = false;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startMessageHandling() {
		ServerMessageHandler serverMessageHandler = new ServerMessageHandler(serverName+"MessageHandler", incomingMessages, socket);
		Thread incomingMessagesListHandler = new Thread(serverMessageHandler);
		incomingMessagesListHandler.start();
		
		//Add hotelBroker test message to Queue
		Message hotelBrokerTestMsg = new Message(StatusTypes.TESTING, hotelBrokerAddress.toString(), hotelBrokerPort, 0, "HiFromHotel");
		incomingMessages.add(hotelBrokerTestMsg);
		logger.trace("Added Message to "+ serverName +"Queue: <"+ hotelBrokerTestMsg.toString()+ ">");
		
		//Add hotelBroker test message to Queue
		Message carBrokerTestMsg = new Message(StatusTypes.TESTING, carBrokerAddress.toString(), carBrokerPort, 0, "HiFromCarBroker");
		incomingMessages.add(carBrokerTestMsg);
		logger.trace("Added Message to "+ serverName +"Queue: <"+ carBrokerTestMsg.toString() + ">");
		
		while(online) {
			try {
				socket.setSoTimeout(120000);
			    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			    socket.receive(packet);
			    String received = new String(packet.getData(), 0, packet.getLength());
			    logger.trace("Server received: "+ received);
			    Message msg = new Message(received);
			    if(msg.validate()) {
			    	logger.trace("Received Message has a valid form: <" + msg.toString() +">");
			    	try {
						incomingMessages.put(msg);
						logger.trace("Added Message to "+ serverName +"Queue: <" + msg.toString() +">");
					} catch (InterruptedException e) {
						e.printStackTrace();
						logger.trace("ServerQueue <" + serverName +"> is full!");
					}
			    } else {
			    	logger.trace("Received Message has an invalid form: <" + received +">");
			    	msg = new Message(StatusTypes.ERROR, localAddress, socket.getLocalPort(), -1, "Error_invalid_message");
			    	packet = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, hotelBrokerAddress, hotelBrokerPort);
				    socket.send(packet);
			    }
		    } catch (SocketTimeoutException e) {
	            //Timeout
		    	logger.trace("ServerSocket <" + serverName +"> timeout (no message received)!");
	        } catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
