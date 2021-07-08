package Message;

import java.net.InetAddress;

public class MessageFactory {
    //Attribute

    public Message buildPrepare(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.PREPARE, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildReady(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.READY, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildAbort(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.ABORT, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildAcknowledge(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.ACKNOWLEDGMENT, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildRollback(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.ROLLBACK, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildCommit(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.COMMIT, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildError(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.ERROR, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildInquire(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.INQUIRE, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildInfoCars(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.INFOCARS, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildInfoRooms(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.INFOROOMS, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildInfo(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.INFO, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildTest(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.TESTING, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }

    public Message buildConnectionTest(String pBookingIdString, String pContent, InetAddress pSenderAddress, int pSenderPort) {
        return new Message(StatusTypes.CONNECTIONTEST, pSenderAddress, pSenderPort, pBookingIdString, pContent);
    }
}
