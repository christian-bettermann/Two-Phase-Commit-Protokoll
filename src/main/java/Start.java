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
		Configurator.setRootLevel(Level.INFO);

		logger.info("Starting System...");
		int clientOnePort = 33091;
		int clientTwoPort = 33092;
		
		//Start Brokers
		CarBroker carBroker = new CarBroker();
		HotelBroker hotelBroker = new HotelBroker();
		Server serverOne = new Server(1);
		Server serverTwo = new Server(2);
		Client clientOne = new Client(clientOnePort);
		Client clientTwo = new Client(clientTwoPort);
		
		Thread serverOneThread = new Thread(serverOne);
		Thread serverTwoThread = new Thread(serverTwo);
		Thread carBrokerThread = new Thread(carBroker);
		Thread hotelBrokerThread = new Thread(hotelBroker);
		Thread clientOneThread = new Thread(clientOne);
		Thread clientTwoThread = new Thread(clientTwo);
		
		ControlPanel controlPanel = new ControlPanel(serverOne, serverTwo, carBroker, hotelBroker, serverOneThread, serverTwoThread, carBrokerThread, hotelBrokerThread) ;
		Thread controlPanelThread = new Thread(controlPanel);
		
		serverOneThread.start();
		serverTwoThread.start();
		carBrokerThread.start();
		hotelBrokerThread.start();
		clientOneThread.start();
		clientTwoThread.start();
		controlPanelThread.start();
	}
}
