import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import CarService.CarBroker;
import HotelService.HotelBroker;

public class Start {
	private static final Logger logger = LogManager.getRootLogger();
	
	public static void main(String[] args) {
		//init logger
		Configurator.setRootLevel(Level.TRACE);

		logger.info("Starting System...");
		int clientPort = 33091;
		
		//Start Brokers
		CarBroker carBroker = new CarBroker();
		HotelBroker hotelBroker = new HotelBroker();
		Server serverOne = new Server(1);
		Server serverTwo = new Server(2);
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
