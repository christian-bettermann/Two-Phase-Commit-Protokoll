
public class Start {
	public static void main(String[] args) {
		int firstServerPort = 30800;
		int secondServerPort = 30801;
		int carBrokerPort = 30901;
		int hotelBrokerPort = 30902;
		Server server1 = new Server(firstServerPort, carBrokerPort, hotelBrokerPort);
		Server server2 = new Server(secondServerPort, carBrokerPort, hotelBrokerPort);
	}
}
