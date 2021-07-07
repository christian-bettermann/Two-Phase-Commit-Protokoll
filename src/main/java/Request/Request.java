package Request;

import java.net.InetAddress;
import java.util.Date;

public abstract class Request {
    //Attribute
    protected InetAddress targetIp;
    protected int targetPort;
    protected int id;
    protected Date startTime;
    protected Date endTime;

    public InetAddress getTargetIp() {
        return targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public int getId() {
        return id;
    }

    public String getIdAsString() {
        String result = "" + this.id + "";
        return result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }
}
