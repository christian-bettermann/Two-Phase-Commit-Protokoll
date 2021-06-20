package Message;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Message {
	private static final Logger logger = LogManager.getRootLogger();
	private StatusTypes status;
	private InetAddress senderAddress;
	private int senderPort; 
	private int bookingID; 
	private String statusMessage;

	public Message(StatusTypes status, String senderAddress, int senderPort, int bookingID, String statusMessage) {
		this.status = status;
		try {
			this.senderAddress = InetAddress.getByName(senderAddress.toString().split("/")[1]);	//
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.senderPort = senderPort; 
		this.bookingID = bookingID; 
		this.statusMessage = statusMessage;
	}
	
	public Message(StatusTypes status, InetAddress senderAddress, int senderPort, int bookingID, String statusMessage) {
		this.status = status;
		this.senderAddress = senderAddress;
		this.senderPort = senderPort; 
		this.bookingID = bookingID; 
		this.statusMessage = statusMessage;
	}
	
	public Message() {
		
	}
	
	public Message(String msg) {
		String[] msgArray = msg.split(" ");
		if(msgArray.length == 5) {
			try {
				status = StatusTypes.valueOf(msgArray[0].trim());
				
				senderAddress = InetAddress.getByName(msgArray[1].trim().split("/")[1]);
				logger.warn(senderAddress);
				senderPort = Integer.parseInt(msgArray[2].trim());
				bookingID = Integer.parseInt(msgArray[3].trim());
				statusMessage = msgArray[4].trim();
			} catch(Exception e) {
				e.printStackTrace();
				status = StatusTypes.ERROR;
				senderAddress = null;
				senderPort = -1; 
				bookingID = -1; 
				statusMessage = null;
			}
			logger.trace("Built Message Object(status:<" + status + ">, senderAddress:<" + senderAddress + ">, senderPort:<" + senderPort + ">, bookingID:<" + bookingID + ">, statusMessage:<" + statusMessage + ">");
		} else {
			logger.error("Message building failed for message: <" + msg + ">");
		}
	}
	
	public String toString() {
		return status + " " + senderAddress + " " + senderPort + " " + bookingID + " " + statusMessage;
	}
	
	public boolean validate() {
		if(status != StatusTypes.ERROR) {
			return true;
		}
		return false;
	}
	
	
	public StatusTypes getStatus() {
		return status;
	}

	public void setStatus(StatusTypes status) {
		this.status = status;
	}

	public InetAddress getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(InetAddress senderAdress) {
		this.senderAddress = senderAdress;
	}

	public int getSenderPort() {
		return senderPort;
	}

	public void setSenderPort(int senderPort) {
		this.senderPort = senderPort;
	}

	public int getBookingID() {
		return bookingID;
	}

	public void setBookingID(int bookingID) {
		this.bookingID = bookingID;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
}