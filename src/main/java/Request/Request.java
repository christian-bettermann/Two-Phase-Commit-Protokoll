package Request;

import java.util.Date;

public abstract class Request {
    //Attribute
    protected String id;
    protected Date startTime;
    protected Date endTime;


    public String getId() {
        return id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }
}
