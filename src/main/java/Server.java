import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class Server {
	
	private static DatagramSocket socket;
    private static boolean running;
    private byte[] buffer = new byte[256];
    
    private static InetAddress carBrokerAddress;
    private static InetAddress hotelBrokerAddress;
	
    private static DatagramSocket carBrokerSocket;
    private static DatagramSocket hotelBrokerSocket;
    
    static int carBrokerPort;
    static int hotelBrokerServer;
    
    static boolean carBrokerOnline, hotelBrokerOnline;
    
	public Server (int serverPort, InetAddress carBrokerAddress, int carBrokerPort, InetAddress hotelBrokerAddress, int hotelBrokerServer) {
		this.carBrokerAddress = carBrokerAddress;
		this.hotelBrokerAddress = hotelBrokerAddress;
		carBrokerOnline = false;

		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		//Sending initial Messages to Broker

		//initial CarBroker message
		do {
			checkCarBrokerAvailability();
			if(!carBrokerOnline) {
				try {
					TimeUnit.SECONDS.sleep(5);
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
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (!hotelBrokerOnline);
		
		//Brokers are initialy available
	
		
		
		
		
	}
	
	public static void checkCarBrokerAvailability() {
		try {
			byte[] buf;
			String msg = "0 InitialMessageRequest";
			
		    carBrokerSocket = new DatagramSocket();
		    buf = msg.getBytes();
		    DatagramPacket packet = new DatagramPacket(buf, buf.length, carBrokerAddress, carBrokerPort);
		    carBrokerSocket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buf, buf.length);
		    carBrokerSocket.receive(packet);
		    String response = new String(packet.getData(), 0, packet.getLength());
		    if(response.equals("0 InitialMessageResponse")) {
		    	carBrokerOnline = true;
		    }
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void checkHotelBrokerAvailability() {
		try {
			byte[] buf;
			String msg = "0 InitialMessageRequest";
			
		    hotelBrokerSocket = new DatagramSocket();
		    buf = msg.getBytes();
		    DatagramPacket packet = new DatagramPacket(buf, buf.length, carBrokerAddress, carBrokerPort);
		    hotelBrokerSocket.send(packet);
		    //wait for answer
		    packet = new DatagramPacket(buf, buf.length);
		    hotelBrokerSocket.receive(packet);
		    String response = new String(packet.getData(), 0, packet.getLength());
		    if(response.equals("0 InitialMessageResponse")) {
		    	hotelBrokerOnline = true;
		    }
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
}
