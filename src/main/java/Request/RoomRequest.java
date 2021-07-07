package Request;

import java.net.InetAddress;
import java.util.Date;

public class RoomRequest extends Request{
    //Attribute
    private int roomId;

    public RoomRequest(InetAddress pTargetIp, int pTargetPort, int pBookingId, int pRoomId, Date pStartTime, Date pEndTime) {
        this.targetIp = pTargetIp;
        this.targetPort = pTargetPort;
        this.id = pBookingId;
        this.roomId = pRoomId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getRoomId() {
        return this.roomId;
    }
}
