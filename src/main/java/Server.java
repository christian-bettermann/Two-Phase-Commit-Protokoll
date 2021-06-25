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
    private InetAddress localAddress;
    int serverPort;
    boolean brokerToCheckOnline;
    Broker[] broker = new Broker[2];
    
	public Server (String serverName, int serverPort, InetAddress carBrokerAddress, int carBrokerPort, InetAddress hotelBrokerAddress, int hotelBrokerPort) {
		logger.trace("Creating Server <" + serverName + ">...");
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		incomingMessages = new ArrayBlockingQueue<Message>(1024);
		this.serverName = serverName;
		this.serverPort = serverPort;
		brokerToCheckOnline = false;
		
		Broker car = new Broker("CarBroker", carBrokerAddress, carBrokerPort);
		Broker hotel = new Broker("HotelBroker" ,hotelBrokerAddress, hotelBrokerPort);
		broker[0] = car;
		broker[1] = hotel;
	}
	
	public void run() {
		logger.info("Starting Server <" + serverName + "> on port <" + serverPort + "> ...");
		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//Sending initial Messages to Brokers
		for(Broker b : broker) {
			do {
				checkBrokerAvailability(b);
				if(!brokerToCheckOnline) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} while (!brokerToCheckOnline);
		}
		//Brokers are initialy available
		startMessageHandling();
	}
	
	public void checkBrokerAvailability(Broker brokerToCheck) {
		try {
			brokerToCheckOnline = false;
			Message msg = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), 0, "InitialMessageRequest");
			socket.setSoTimeout(2500);
		    DatagramPacket packet = new DatagramPacket(msg.toString().getBytes(), msg.toString().getBytes().length, brokerToCheck.getAddress(), brokerToCheck.getPort());
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buffer, buffer.length);
		    socket.receive(packet);
		    String premsg = new String(packet.getData(), 0, packet.getLength());
		    Message received = new Message(premsg);
		    logger.trace("Server received: "+ received.toString());
		    if(received.getStatus() == StatusTypes.CONNECTIONTEST && received.getSenderAddress().getHostAddress().equals(brokerToCheck.getAddress().getHostAddress()) && received.getSenderPort() == brokerToCheck.getPort()) {
		    	brokerToCheckOnline = true;
		    	logger.info(brokerToCheck.getName() + " is available for <" + serverName +">");
		    }
	    } catch (SocketTimeoutException e) {
            //Timeout
	    	logger.error(brokerToCheck.getName() + " is not available for <" + serverName +">!");
	    	brokerToCheckOnline = false;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startMessageHandling() {
		ServerMessageHandler serverMessageHandler = new ServerMessageHandler(serverName+"MessageHandler", incomingMessages, socket, this);
		Thread incomingMessagesListHandler = new Thread(serverMessageHandler);
		incomingMessagesListHandler.start();
		
		//Add hotelBroker test message to Queue
		Message hotelBrokerTestMsg = new Message(StatusTypes.TESTING, broker[1].getAddress(), broker[1].getPort(), 0, "HiFromHotel");
		incomingMessages.add(hotelBrokerTestMsg);
		logger.trace("Added Message to "+ serverName +"Queue: <"+ hotelBrokerTestMsg.toString()+ ">");
		
		//Add carBroker test message to Queue
		Message carBrokerTestMsg = new Message(StatusTypes.TESTING, broker[0].getAddress(), broker[0].getPort(), 0, "HiFromCarBroker");
		incomingMessages.add(carBrokerTestMsg);
		logger.trace("Added Message to "+ serverName +"Queue: <"+ carBrokerTestMsg.toString() + ">");
		
		while(online) {
			try {
				socket.setSoTimeout(120000);
			    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			    socket.receive(packet);
			    String received = new String(packet.getData(), 0, packet.getLength());
			    logger.trace("Server received: "+ received);
			    Message msgIn = new Message(received);
			    if(msgIn.validate()) {
			    	logger.trace("Received Message has a valid form: <" + msgIn.toString() +">");
			    	try {
						incomingMessages.put(msgIn);
						logger.trace("Added Message to "+ serverName +"Queue: <" + msgIn.toString() +">");
					} catch (InterruptedException e) {
						e.printStackTrace();
						logger.trace("ServerQueue <" + serverName +"> is full!");
					}
			    } else {
			    	logger.trace("Received Message has an invalid form: <" + received +">");
			    	Message msgOut = new Message(StatusTypes.ERROR, localAddress, socket.getLocalPort(), -1, "Error_invalid_message");
			    	packet = new DatagramPacket(msgOut.toString().getBytes(), msgOut.toString().getBytes().length, msgIn.getSenderAddress(), msgIn.getSenderPort());
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
	
	public Broker[] getBroker() {
		return broker;
	}
}
