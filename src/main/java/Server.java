import java.io.FileNotFoundException;
import java.io.FileReader;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server implements Runnable {
	//Attribute
	private int id;
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
    
	public Server (int id) {
		this.id = id;
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		Broker car;
		Broker hotel;
		InetAddress carBrokerAddress;
		InetAddress hotelBrokerAddress;
		int carBrokerPort;
		int hotelBrokerPort;
		JSONParser jParser = new JSONParser();
		try (FileReader reader = new FileReader("src/main/resources/Server/config_Server_" + id + ".json"))
		{
			Object jsonContent = jParser.parse(reader);
			JSONObject configData = (JSONObject) jsonContent;
			this.serverName = configData.get("serviceName").toString();
			logger.trace("Creating Server <" + serverName + ">...");
			this.localAddress = InetAddress.getByName(configData.get("ip").toString());
			this.serverPort = Integer.parseInt(configData.get("port").toString());
			carBrokerAddress = InetAddress.getByName(configData.get("carBrokerIp").toString());
			hotelBrokerAddress = InetAddress.getByName(configData.get("hotelBrokerIp").toString());
			carBrokerPort = Integer.parseInt(configData.get("carBrokerPort").toString());
			hotelBrokerPort = Integer.parseInt(configData.get("hotelBrokerPort").toString());
			car = new Broker("CarBroker", carBrokerAddress, carBrokerPort);
			hotel = new Broker("HotelBroker", hotelBrokerAddress, hotelBrokerPort);
			broker[0] = car;
			broker[1] = hotel;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		incomingMessages = new ArrayBlockingQueue<Message>(1024);
		brokerToCheckOnline = false;
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
			Message msg = new Message(StatusTypes.CONNECTIONTEST, localAddress, socket.getLocalPort(), "0", "InitialMessageRequest");
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
		ServerMessageHandler serverMessageHandler = new ServerMessageHandler(this.id,serverName+"MessageHandler", incomingMessages, socket, this);
		Thread incomingMessagesListHandler = new Thread(serverMessageHandler);
		incomingMessagesListHandler.start();
		
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
			    	Message msgOut = new Message(StatusTypes.ERROR, localAddress, socket.getLocalPort(), null, "Error_invalid_message");
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
	
	public String getName() {
		return serverName;
	}
	
	public void closeSocket() {
		socket.close();
	}
}
