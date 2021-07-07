package Request;

import Message.StatusTypes;

import java.util.Date;

public class ServerRequest extends Request{
    //Attribute
    private int carId;
    private int roomId;

    public ServerRequest(String pBookingId, int pCarId, int pRoomId, Date pStartTime, Date pEndTime) {
        this.id = pBookingId;
        this.carId = pCarId;
        this.roomId = pRoomId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getCarId() {
        return this.carId;
    }

    public int getRoomId() {
        return this.roomId;
    }
}
