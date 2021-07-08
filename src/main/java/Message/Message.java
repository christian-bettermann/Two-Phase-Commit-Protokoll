package Message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Message {
	private static final Logger logger = LogManager.getRootLogger();
	private StatusTypes status;
	private InetAddress senderAddress;
	private int senderPort; 
	private String bookingID; 
	private String statusMessage;

	public Message(StatusTypes status, String senderAddress, int senderPort, String bookingID, String statusMessage) {
		this.status = status;
		try {
			this.senderAddress = InetAddress.getByName(senderAddress.split("/")[1]);	//
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.senderPort = senderPort; 
		this.bookingID = bookingID; 
		this.statusMessage = statusMessage.replace(" ", "_");
	}
	
	public Message(StatusTypes status, InetAddress senderAddress, int senderPort, String bookingID, String statusMessage) {
		this.status = status;
		this.senderAddress = senderAddress;
		this.senderPort = senderPort; 
		this.bookingID = bookingID; 
		this.statusMessage = statusMessage.replace(" ", "_");
	}

	public Message(String msg, InetAddress pSenderAddress, int pSenderPort) {
		this.senderAddress = pSenderAddress;
		this.senderPort = pSenderPort;
		String[] msgArray = msg.split(" ");
		if(msgArray.length == 5) {
			try {
				status = StatusTypes.valueOf(msgArray[0].trim());
				if(msgArray[1].trim().split("/").length > 1) {
					senderAddress = InetAddress.getByName(msgArray[1].trim().split("/")[1]);
				} else {
					senderAddress = InetAddress.getByName(msgArray[1].trim());
				}
				senderPort = Integer.parseInt(msgArray[2].trim());
				bookingID = msgArray[3].trim();
				statusMessage = msgArray[4].trim();
			} catch(Exception e) {
				e.printStackTrace();
				status = StatusTypes.ERROR;
				senderAddress = null;
				senderPort = -1;
				bookingID = null;
				statusMessage = null;
			}
			logger.trace("Built Message Object(status:<" + status + ">, senderAddress:<" + senderAddress + ">, senderPort:<" + senderPort + ">, bookingID:<" + bookingID + ">, statusMessage:<" + statusMessage + ">");
		} else {
			logger.error("Message building failed for message: <" + msg + ">");
		}
	}

	public Message(String msg) {
		String[] msgArray = msg.split(" ");
		if(msgArray.length == 5) {
			try {
				status = StatusTypes.valueOf(msgArray[0].trim());
				if(msgArray[1].trim().split("/").length > 1) {
					senderAddress = InetAddress.getByName(msgArray[1].trim().split("/")[1]);
				} else {
					senderAddress = InetAddress.getByName(msgArray[1].trim());
				}
				senderPort = Integer.parseInt(msgArray[2].trim());
				bookingID = msgArray[3].trim();
				statusMessage = msgArray[4].trim();
			} catch(Exception e) {
				e.printStackTrace();
				status = StatusTypes.ERROR;
				senderAddress = null;
				senderPort = -1; 
				bookingID = null; 
				statusMessage = null;
			}
			logger.trace("Built Message Object(status:<" + status + ">, senderAddress:<" + senderAddress + ">, senderPort:<" + senderPort + ">, bookingID:<" + bookingID + ">, statusMessage:<" + statusMessage + ">");
		} else {
			logger.error("Message building failed for message: <" + msg + ">");
		}
	}

	public String toString() {
		if(senderAddress != null && senderAddress.toString().charAt(0) == '/') {
			return status + " " + senderAddress.toString().trim().split("/")[1] + " " + senderPort + " " + bookingID + " " + statusMessage;
		}
		return status + " " + senderAddress + " " + senderPort + " " + bookingID + " " + statusMessage;
	}

	public byte[] getAsBytes() {
		return this.toString().getBytes();
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

	public String getBookingID() {
		return bookingID;
	}

	public void setBookingID(String bookingID) {
		this.bookingID = bookingID;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	public String getStatusMessageCarId() {
		String[] statusMessageArray = statusMessage.split("_");
		if(statusMessageArray.length == 4) {
			return statusMessageArray[0];
		}
		return "ERROR";
	}
	
	public String getStatusMessageRoomId() {
		String[] statusMessageArray = statusMessage.split("_");
		if(statusMessageArray.length == 4) {
			return statusMessageArray[1];
		}
		return "ERROR";
	}
	
	public long getStatusMessageStartTime() {
		String[] statusMessageArray = statusMessage.split("_");
		if(statusMessageArray.length == 4) {
			return Long.parseLong(statusMessageArray[2]);
		}
		return 0;
	}
	
	public long getStatusMessageEndTime() {
		String[] statusMessageArray = statusMessage.split("_");
		if(statusMessageArray.length == 4) {
			return Long.parseLong(statusMessageArray[3]);
		}
		return 0;
	}
}