package Request;

import java.net.InetAddress;
import java.util.Date;

public class CarRequest extends Request{
    //Attribute
    private int carId;

    public CarRequest(InetAddress pTargetIp, int pTargetPort, String pBookingId, int pCarId, Date pStartTime, Date pEndTime) {
        this.targetIp = pTargetIp;
        this.targetPort = pTargetPort;
        this.id = pBookingId;
        this.carId = pCarId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getCarId() {
        return this.carId;
    }
}
