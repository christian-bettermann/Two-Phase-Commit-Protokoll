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
		Configurator.setRootLevel(Level.TRACE);

		logger.info("Starting System...");
		int firstServerPort = 30800;
		int secondServerPort = 30801;
		try {
			carBrokerAddress = InetAddress.getByName("localhost");
			hotelBrokerAddress = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int carBrokerPort = 30901;
		int hotelBrokerPort = 30902;
		int clientPort = 33091;
		
		//Start Brokers
		CarBroker carBroker = new CarBroker();
		HotelBroker hotelBroker = new HotelBroker();
		Server serverOne = new Server("ServerOne", firstServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
		Server serverTwo = new Server("ServerTwo", secondServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
		Client client = new Client(clientPort);
		
		
		Thread serverOneThread = new Thread(serverOne);
		Thread serverTwoThread = new Thread(serverTwo);
		Thread carBrokerThread = new Thread(carBroker);
		Thread hotelBrokerThread = new Thread(hotelBroker);
		Thread clientThread = new Thread(client);
		
		ControlPanel controlPanel = new ControlPanel(serverOne, serverTwo, carBroker, hotelBroker, serverOneThread, serverTwoThread, carBrokerThread, hotelBrokerThread) ;
		Thread controlPanelThread = new Thread(controlPanel);
		
		serverOneThread.start();
		serverTwoThread.start();
		carBrokerThread.start();
		hotelBrokerThread.start();
		clientThread.start();
		controlPanelThread.start();
	}
}
