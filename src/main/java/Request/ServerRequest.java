package Request;

import Message.StatusTypes;

import java.net.InetAddress;
import java.util.Date;

public class ServerRequest extends Request{
    //Attribute
    private int carId;
    private int roomId;
    private int messageCounter;
    private StatusTypes stateOfCarBroker;
    private StatusTypes stateOfHotelBroker;
    private InetAddress clientAddress;
    private int clientPort;

    public ServerRequest(String pBookingId, int pCarId, int pRoomId, Date pStartTime, Date pEndTime, InetAddress clientAddress, int clientPort) {
        this.id = pBookingId;
        this.carId = pCarId;
        this.roomId = pRoomId;
        this.messageCounter = 0;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.stateOfCarBroker = StatusTypes.INITIALIZED;
        this.stateOfHotelBroker = StatusTypes.INITIALIZED;
    }

    public ServerRequest(String pBookingId, int pCarId, int pRoomId, Date pStartTime, Date pEndTime, InetAddress clientAddress, int clientPort, StatusTypes carState, StatusTypes hotelState) {
        this.id = pBookingId;
        this.carId = pCarId;
        this.roomId = pRoomId;
        this.messageCounter = 0;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.stateOfCarBroker = carState;
        this.stateOfHotelBroker = hotelState;
    }

    public int getCarId() {
        return this.carId;
    }

    public int getRoomId() {
        return this.roomId;
    }

    public void setHotelBrokerState(StatusTypes pState) {
        this.stateOfHotelBroker = pState;
        this.messageCounter++;
    }

    public void setCarBrokerState(StatusTypes pState) {
        this.stateOfCarBroker = pState;
        this.messageCounter++;
    }

    public StatusTypes getHotelBrokerState() {
        return this.stateOfHotelBroker;
    }

    public StatusTypes getCarBrokerState() {
        return this.stateOfCarBroker;
    }

    public int getMessageCounter() {
        return this.messageCounter;
    }

    public void resetMessageCounter() {
        this.messageCounter = 0;
    }
    
    public InetAddress getClientAddress() {
		return clientAddress;
	}

	public int getClientPort() {
		return clientPort;
	}

	public boolean bothReady() {
        if(this.stateOfCarBroker.equals(StatusTypes.READY) && this.stateOfHotelBroker.equals(StatusTypes.READY)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean bothAcknowledged() {
        if(this.stateOfCarBroker.equals(StatusTypes.ACKNOWLEDGMENT) && this.stateOfHotelBroker.equals(StatusTypes.ACKNOWLEDGMENT)) {
            return true;
        } else {
            return false;
        }
    }
}
