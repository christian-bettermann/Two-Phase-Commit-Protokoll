import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import CarService.CarBroker;
import HotelService.HotelBroker;

public class Start {
	static InetAddress carBrokerAddress;
	static InetAddress hotelBrokerAddress;
	private static final Logger logger = LogManager.getRootLogger();
	
	public static void main(String[] args) {
		//init logger
		Configurator.setRootLevel(Level.INFO);

		logger.info("Starting System...");
		int firstServerPort = 30800;
		int secondServerPort = 30801;
		try {
			carBrokerAddress = InetAddress.getLocalHost();
			hotelBrokerAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int carBrokerPort = 30901;
		int hotelBrokerPort = 30902;
		
		//Start Brokers
		logger.info("Creating Brokers");
		CarBroker carBroker = new CarBroker(carBrokerPort);
		HotelBroker hotelBroker = new HotelBroker(hotelBrokerPort);
		logger.info("Creating Servers");
		Server server1 = new Server(firstServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
		Server server2 = new Server(secondServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
		
		server1.start();
		server2.start();
		carBroker.start();
		hotelBroker.start();
		
		logger.info("Started Everything!");
	}
}
