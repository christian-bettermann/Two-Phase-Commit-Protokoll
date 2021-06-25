import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import Message.*;


public class Client {
	private static final Logger logger = LogManager.getRootLogger();
	private static InetAddress localAddress;
	
	public static void main(String[] args) {
		Configurator.setRootLevel(Level.INFO);
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		int clientPort = 98735;
		
		String carID = "111";
		String hotelID = "222";
		
		String startTime = "2021-06-25";
		String endTime = "2021-06-27";
		try {
	         Date start = (Date) new SimpleDateFormat("yyyy-MM-dd").parse(startTime);
	         Date end = (Date) new SimpleDateFormat("yyyy-MM-dd").parse(endTime);
	         
	         Message m = new Message(StatusTypes.BOOKING, localAddress, clientPort, 0, buildStatusMessage(carID, hotelID, start.getTime(), end.getTime()));
	         logger.info("The trip will be from "+ new Date(m.getStatusMessageStartTime()) +" until "+ new Date(m.getStatusMessageEndTime()) +". We have reserved the car "+ carID +" and the hotel "+ hotelID +" for you,");
	    } catch (Exception e) {
	         logger.error(e);
	    }
		
		
		
	}
	
	public static String buildStatusMessage(String carID, String hotelID, long start, long end) {
		return carID + "_" + hotelID + "_" + start + "_" + end;
	}
}
