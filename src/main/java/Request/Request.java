package Request;

import Message.StatusTypes;

import java.net.InetAddress;
import java.util.Date;

public abstract class Request {
    //Attribute
    protected InetAddress targetIp;
    protected int targetPort;
    protected String id;
    protected Date startTime;
    protected Date endTime;
    protected StatusTypes state;

    public InetAddress getTargetIp() {
        return targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getId() {
        return id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public StatusTypes getState() {
        return state;
    }
}
