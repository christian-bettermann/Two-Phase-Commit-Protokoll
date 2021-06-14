import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server implements Runnable {
	protected static final Logger logger = LogManager.getRootLogger();
	private static DatagramSocket socket;
	private byte[] buffer = new byte[1024];
	private String serverName;
	
    private InetAddress carBrokerAddress;
    private InetAddress hotelBrokerAddress;
	    
    int serverPort, carBrokerPort, hotelBrokerPort;
    
    boolean carBrokerOnline, hotelBrokerOnline;
    
	public Server (String serverName, int serverPort, InetAddress carBrokerAddress, int carBrokerPort, InetAddress hotelBrokerAddress, int hotelBrokerPort) {
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
		logger.info("Starting Server <" + serverName + ">...");
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
		
		
	}
	
	public void checkCarBrokerAvailability() {
		try {
			String msg = "0 InitialMessageRequest";
			socket.setSoTimeout(2500);

		    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.getBytes().length, carBrokerAddress, carBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buffer, buffer.length);
		    socket.receive(packet);
		    String received = new String(packet.getData(), 0, packet.getLength());
		    logger.trace("Server received: "+ received);
		    if(received.equals("0 InitialMessageResponseCarBroker")) {
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
			String msg = "0 InitialMessageRequest";
			socket.setSoTimeout(2500);
			
		    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.getBytes().length, hotelBrokerAddress, hotelBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buffer, buffer.length);
		    socket.receive(packet);
		    String received = new String(packet.getData(), 0, packet.getLength());
		    logger.trace("Server received: "+ received);
		    if(received.equals("0 InitialMessageResponseHotelBroker")) {
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
}
