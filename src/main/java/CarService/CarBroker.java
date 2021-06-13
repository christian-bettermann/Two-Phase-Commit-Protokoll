package CarService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarBroker {
	protected static final Logger logger = LogManager.getRootLogger();
	private static DatagramSocket socket;
    private boolean online;
    private byte[] buffer = new byte[256];
    
    int carBrokerPort;
    
    public CarBroker(int carBrokerPort) {
    	logger.info("Creating CarBroker...");
    	this.carBrokerPort = carBrokerPort;
    }
    
    public void start() {
    	logger.info("Starting CarBroker...");
    	try {
			socket = new DatagramSocket(carBrokerPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
        online = true;
        
        while (online) {
        	try {
        		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
            
	            InetAddress address = dp.getAddress();
	            int port = dp.getPort();
	            String received = new String(dp.getData(), 0, dp.getLength());
	            
	            //Message handler
	            if (received.equals("0 InitialMessageRequest")) {
	            	String msg = "0 InitialMessageResponseCarBroker";
	    		    buffer = msg.getBytes();
	    		    dp = new DatagramPacket(buffer, buffer.length, address, port);
	            	continue;
	            }
	            socket.send(dp);
        	} catch (IOException e) {
				e.printStackTrace();
			}
        }
        socket.close();
	}
}