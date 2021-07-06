package Request;

import java.util.Date;

public class RoomRequest extends Request{
    //Attribute
    private int roomId;

    public RoomRequest(String pBookingId, int pRoomId, Date pStartTime, Date pEndTime) {
        this.id = pBookingId;
        this.roomId = pRoomId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getRoomId() {
        return this.roomId;
    }
}
