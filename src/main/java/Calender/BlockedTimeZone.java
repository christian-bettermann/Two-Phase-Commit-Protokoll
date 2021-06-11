package Calender;

import java.util.Date;

public class BlockedTimeZone {

    //Attribute
    private Date startTime;
    private Date endTime;

    public BlockedTimeZone(Date pStart, Date pEnd) {
        this.startTime = pStart;
        this.endTime = pEnd;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
