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
	private int id;															//id can be 1 or 2; indicates which config file is used for the server setup
	private static final Logger logger = LogManager.getRootLogger();		//shared logger
	private DatagramSocket socket;											//UPD socket
	private byte[] buffer = new byte[1024];									//message buffer
	private String serverName;												//loaded from config files stored globally
	private boolean online = true;											//keeps the while loop for receiving messages alive
	private BlockingQueue<Message> incomingMessages;						//shared Queue for messages (every received message is passed to the ServerMessageHandler through this queue)
    private InetAddress localAddress;										//own ip
    int serverPort;															//own port
    boolean brokerToCheckOnline;											//used by the broker connection test on every server setup
    Broker carBroker;														//only stores information (like address & port) about the broker (this is not the actual broker)
    Broker hotelBroker;														//only stores information (like address & port) about the broker (this is not the actual broker)
    Thread incomingMessagesListHandler;										//Thread of the ServerMessageHandler
    ServerMessageHandler serverMessageHandler;								//the ServerMessageHandler receives the messages from the server an handles the response
    
    /**
	 * A constructor to create a new Server
	 * @param	id: the id of the server, so it knows which config file to use
	 */
	public Server (int id) {
		this.id = id;
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		InetAddress carBrokerAddress;
		InetAddress hotelBrokerAddress;
		int carBrokerPort;
		int hotelBrokerPort;
		//load server configuration from the config file
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
			this.carBroker = new Broker("CarBroker", carBrokerAddress, carBrokerPort);
			this.hotelBroker = new Broker("HotelBroker", hotelBrokerAddress, hotelBrokerPort);
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		//create shared queue for the incomming messages
		incomingMessages = new ArrayBlockingQueue<Message>(1024);
		brokerToCheckOnline = false;
	}
	
	//A function to start the server
	public void run() {
		logger.info("Starting Server <" + serverName + "> on port <" + serverPort + "> ...");
		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		Broker[] broker = {carBroker, hotelBroker};
		//sending initial messages to brokers to proof, that they are online
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
	
	/**
	 * A method to check if a broker is online or shutdown
	 * @param broker:	Broker to check the connection to
	 */
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
	
	//A method to start receiving messages
	public void startMessageHandling() {
		//starting the ServerMessageHandler
		serverMessageHandler = new ServerMessageHandler(this.id,serverName+"MessageHandler", incomingMessages, socket, this);
		incomingMessagesListHandler = new Thread(serverMessageHandler);
		incomingMessagesListHandler.start();
		
		//start receiving
		while(online) {
			try {
				socket.setSoTimeout(120000);
			    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			    socket.receive(packet);
			    String received = new String(packet.getData(), 0, packet.getLength());
			    logger.trace("Server received: "+ received);
			    Message msgIn = new Message(received);
			    //validate the received message (correct format?)
			    if(msgIn.validate()) {
			    	logger.trace("Received Message has a valid form: <" + msgIn.toString() +">");
			    	try {
			    		//pass message to ServerMessageHandler via shared queue
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
	
	public Broker getCarBroker() {
		return carBroker;
	}

	public Broker getHotelBroker() {
		return hotelBroker;
	}
	
	public String getName() {
		return serverName;
	}
	
	public void closeSocket() {
		socket.close();
	}
	
	//This function is used to shutdown the ServerMessageHandler of a server
	public void shutdownHandler() {
		serverMessageHandler.shutdownServerMessageTimeHandler();
		incomingMessagesListHandler.stop();
	}
}
