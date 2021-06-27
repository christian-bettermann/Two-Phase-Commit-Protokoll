import java.net.InetAddress;

public class Broker {
	private InetAddress address;
	private int port;
	private String name;
	
	public Broker(String name, InetAddress address, int port) {
		this.name = name;
		this.address = address;
		this.port = port;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
