import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import Message.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerMessageHandler implements Runnable{
	private static final Logger logger = LogManager.getRootLogger();
	private DatagramSocket socket;
	private BlockingQueue<Message> incomingMessages;
	private String name;
	private Boolean online;
	
	
	public ServerMessageHandler(String name, BlockingQueue<Message> incomingMessages, DatagramSocket socket) {
		this.incomingMessages = incomingMessages;
		this.name = name;
		this.socket = socket;
	}
	
	public void run() {
		//get messages from queue
		//handle messages
		logger.info("Starting <" + name + "> for port <" + socket.getLocalPort() + ">...");
		online = true;
		while (online) {
        	try {
				Message inMsg = incomingMessages.take();
				logger.info("<" + name + "> removed Message from Queue: <"+ inMsg.toString() +">");
				Message outMsg = this.analyzeAndGetResponse(inMsg);
				if(outMsg != null) {
					DatagramPacket packet = new DatagramPacket(outMsg.toString().getBytes(), outMsg.toString().getBytes().length, inMsg.getSenderAddress(), inMsg.getSenderPort());
					logger.trace("<" + name + "> sent: <"+ new String(packet.getData(), 0, packet.getLength()) +">");
					socket.send(packet);
				}
				
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        socket.close();
	}
	
	private Message analyzeAndGetResponse(Message msg) {
		String statusMessage = msg.getStatusMessage();
		Message response = new Message();
		try {
			switch(msg.getStatus()) {
				case 0:
					if(statusMessage.equals("InitialMessageRequest")) {
						response = new Message(0, InetAddress.getLocalHost(), socket.getLocalPort(), 0, "InitialMessageResponseServerMessageHandler");
					}
					break;
				case 1:
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
					break;
				case 8:
					if(statusMessage.equals("HiFromCarBroker") || statusMessage.equals("HiFromHotel")) {
						response = new Message(8, InetAddress.getLocalHost(), socket.getLocalPort(), 0, "HiFromServerMessageHandler");
					}
					if(statusMessage.equals("OK")) {
						logger.info("Finished test");
						response = null;
					}
					break;
				default:
					response = new Message(-1, InetAddress.getLocalHost(), socket.getLocalPort(), 9, "ERROR ID_FormatException");
					break;
			} 
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return response;
	}
}
