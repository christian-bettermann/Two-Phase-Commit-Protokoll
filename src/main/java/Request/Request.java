package Request;

import java.net.InetAddress;
import java.util.Date;

public abstract class Request {
    //Attribute
<<<<<<< HEAD
    protected InetAddress targetIp;
    protected int targetPort;
    protected int id;
=======
    protected String id;
>>>>>>> 16cead524efdd10fc025577fa2ad523e2e7cd54c
    protected Date startTime;
    protected Date endTime;

    public InetAddress getTargetIp() {
        return targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getId() {
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
