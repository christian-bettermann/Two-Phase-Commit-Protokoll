import java.net.InetAddress;
import java.net.UnknownHostException;

public class Start {
	static InetAddress carBrokerAddress;
	static InetAddress hotelBrokerAddress;
	
	public static void main(String[] args) {
		int firstServerPort = 30800;
		int secondServerPort = 30801;
		try {
			InetAddress carBrokerAddress = InetAddress.getByName("localhost");
			InetAddress hotelBrokerAddress = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int carBrokerPort = 30901;
		int hotelBrokerPort = 30902;
	
		//Start Broker
		
		Server server1 = new Server(firstServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
		Server server2 = new Server(secondServerPort, carBrokerAddress, carBrokerPort, hotelBrokerAddress, hotelBrokerPort);
	}
}
