package Request;

import Message.StatusTypes;

import java.net.InetAddress;
import java.util.Date;

public class CarRequest extends Request{
    //Attribute
    private int carId;

    public CarRequest(InetAddress pTargetIp, int pTargetPort, String pBookingId, int pCarId, Date pStartTime, Date pEndTime, StatusTypes pState) {
        this.targetIp = pTargetIp;
        this.targetPort = pTargetPort;
        this.id = pBookingId;
        this.carId = pCarId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.state = pState;
    }

    public int getCarId() {
        return this.carId;
    }
}
