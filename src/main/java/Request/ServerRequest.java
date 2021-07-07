package Request;

import Message.StatusTypes;

import java.util.Date;

public class ServerRequest extends Request{
    //Attribute
    private int carId;
    private int roomId;
    private StatusTypes stateOfCarBroker;
    private StatusTypes stateOfHotelBroker;

    public ServerRequest(String pBookingId, int pCarId, int pRoomId, Date pStartTime, Date pEndTime) {
        this.id = pBookingId;
        this.carId = pCarId;
        this.roomId = pRoomId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.stateOfCarBroker = StatusTypes.INITIALIZED;
        this.stateOfHotelBroker = StatusTypes.INITIALIZED;
    }

    public int getCarId() {
        return this.carId;
    }

    public int getRoomId() {
        return this.roomId;
    }

    public void setHotelBrokerState(StatusTypes pState) {
        this.stateOfHotelBroker = pState;
    }

    public void setCarBrokerState(StatusTypes pState) {
        this.stateOfCarBroker = pState;
    }

    public StatusTypes getHotelBrokerState() {
        return this.stateOfHotelBroker;
    }

    public StatusTypes getCarBrokerState() {
        return this.stateOfCarBroker;
    }

    public boolean bothReady() {
        if(this.stateOfCarBroker.equals(this.stateOfHotelBroker)) {
            return true;
        } else {
            return false;
        }
    }
}
