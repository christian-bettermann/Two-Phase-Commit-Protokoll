package Request;

import java.util.Date;

public class Request {
    //Attribute
    private int id;
    private Date startTime;
    private Date endTime;

    public Request(int pId, Date pStartTime , Date pEndTime) {
        this.id = pId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }
}
