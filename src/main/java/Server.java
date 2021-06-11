import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {
	protected static final Logger logger = LogManager.getRootLogger();
	private static DatagramSocket socket;
	
    private InetAddress carBrokerAddress;
    private InetAddress hotelBrokerAddress;
	    
    int serverPort, carBrokerPort, hotelBrokerPort;
    
    boolean carBrokerOnline, hotelBrokerOnline;
    
	public Server (int serverPort, InetAddress carBrokerAddress, int carBrokerPort, InetAddress hotelBrokerAddress, int hotelBrokerPort) {
		logger.info("Creating Server...");
		this.serverPort = serverPort;
		this.carBrokerAddress = carBrokerAddress;
		this.hotelBrokerAddress = hotelBrokerAddress;
		this.carBrokerPort = carBrokerPort;
		this.hotelBrokerPort = hotelBrokerPort;
		carBrokerOnline = false;
		hotelBrokerOnline = false;
	}
	

	public void start() {
		logger.info("Starting Server!");
		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//Sending initial Messages to Brokers
		//initial CarBroker message
		do {
			checkCarBrokerAvailability();
		} while (!carBrokerOnline);
		System.out.println("CarBroker available");
		
		//initial HotelBroker message
		do {
			checkHotelBrokerAvailability();
		} while (!hotelBrokerOnline);
		System.out.println("HotelBroker available");
		
		//Brokers are initialy available
		
		
	}
	
	public void checkCarBrokerAvailability() {
		try {
			byte[] buf;
			String msg = "0 InitialMessageRequest";
			
			socket.setSoTimeout(2500);
		    buf = msg.getBytes();
		    DatagramPacket packet = new DatagramPacket(buf, buf.length, carBrokerAddress, carBrokerPort);
		    logger.info(buf+", "+buf.length+", "+carBrokerAddress+", "+carBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buf, buf.length);
		    socket.receive(packet);
		    String response = new String(packet.getData(), 0, packet.getLength());
		    if(response.equals("0 InitialMessageResponseCarBroker")) {
		    	carBrokerOnline = true;
		    }
	    } catch (SocketTimeoutException e) {
            //Timeout
            System.out.println("CarBroker Timeout!");
            carBrokerOnline = false;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void checkHotelBrokerAvailability() {
		try {
			byte[] buf;
			String msg = "0 InitialMessageRequest";
			
			socket.setSoTimeout(2500);
		    buf = msg.getBytes();
		    DatagramPacket packet = new DatagramPacket(buf, buf.length, hotelBrokerAddress, hotelBrokerPort);
		    socket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buf, buf.length);
		    socket.receive(packet);
		    String response = new String(packet.getData(), 0, packet.getLength());
		    if(response.equals("0 InitialMessageResponseHotelBroker")) {
		    	hotelBrokerOnline = true;
		    }
	    } catch (SocketTimeoutException e) {
            //Timeout
            System.out.println("HotelBroker Timeout!");
            hotelBrokerOnline = false;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
}
