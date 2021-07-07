package Request;

import Message.StatusTypes;

import java.net.InetAddress;
import java.util.Date;

public class RoomRequest extends Request{
    //Attribute
    private int roomId;

    public RoomRequest(InetAddress pTargetIp, int pTargetPort, String pBookingId, int pRoomId, Date pStartTime, Date pEndTime, StatusTypes pState) {
        this.targetIp = pTargetIp;
        this.targetPort = pTargetPort;
        this.id = pBookingId;
        this.roomId = pRoomId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.state = pState;
    }

    public int getRoomId() {
        return this.roomId;
    }
}
