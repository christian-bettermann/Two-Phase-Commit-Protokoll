package Request;

import java.util.Date;

public class CarRequest extends Request{
    //Attribute
    private int carId;

    public CarRequest(int pBookingId, int pCarId, Date pStartTime, Date pEndTime) {
        this.id = pBookingId;
        this.carId = pCarId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getCarId() {
        return this.carId;
    }
}
