package Request;

import java.util.Date;

public class Request {
    //Attribute
    private int id;
    private int interestId;
    private Date startTime;
    private Date endTime;

    public Request(int pId, int pInterestId, Date pStartTime , Date pEndTime) {
        this.id = pId;
        this.interestId = pInterestId;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
    }

    public int getId() {
        return id;
    }

    public int getInterestId() {
        return interestId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }
}
