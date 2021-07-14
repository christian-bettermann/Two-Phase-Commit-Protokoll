import Message.*;
import Request.ServerRequest;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.Date;

public class DecisionHandler {
    //Attribute
    private ServerMessageHandler msgHandler;
    private MessageFactory msgFactory;
    private final Logger logger;

    public DecisionHandler(ServerMessageHandler pHandler, Logger pLogger, MessageFactory pMsgFactory) {
        this.msgHandler = pHandler;
        this.msgFactory = pMsgFactory;
        this.logger = pLogger;
    }

    public Message handleReady(Message income, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = income.getBookingID();
        if (relatedRequest == null) {
            logger.trace("2PC already finished, throw away message: " + income.toString());
            return answer;
        }
        if (messageFromCarBroker(income.getSenderAddress(), income.getSenderPort())) {
            if (relatedRequest.getCarBrokerState().equals(StatusTypes.READY)) {
                logger.trace("Already had that information, throw away message: " + income.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### CARBROKER MESSAGE READY!");
            msgHandler.updateRequestAtList(bookingId, StatusTypes.READY, null, null);
            if (relatedRequest.bothReady()) {
                logger.info("### RESULT => COMMIT!");
                relatedRequest.resetMessageCounter();
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.COMMIT);
                Message answerForHotelBroker = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
                answer = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            } else if (relatedRequest.getHotelBrokerState().equals(StatusTypes.ABORT) && relatedRequest.getMessageCounter() >= 2) {
                logger.info("### RESULT => ROLLBACK!");
                relatedRequest.resetMessageCounter();
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
                Message answerForHotelBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
                answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            }
        }
        if(messageFromHotelBroker(income.getSenderAddress(), income.getSenderPort())) {
            if(relatedRequest.getHotelBrokerState().equals(StatusTypes.READY)) {
                logger.trace("Already had that information, throw away message: " + income.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### HOTELBROKER MESSAGE READY!");
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.READY, null);
            if(relatedRequest.bothReady()) {
                logger.info("### RESULT => COMMIT!");
                relatedRequest.resetMessageCounter();
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.COMMIT);
                Message answerForCarBroker = msgFactory.buildCommit(income.getBookingID(), "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
                answer = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            } else if(relatedRequest.getCarBrokerState().equals(StatusTypes.ABORT) && relatedRequest.getMessageCounter() >= 2) {
                logger.info("### RESULT => ROLLBACK!");
                relatedRequest.resetMessageCounter();
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
                Message answerForCarBroker = msgFactory.buildRollback(income.getBookingID(), "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
                answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            }
        }
        return answer;
    }

    public Message handleAbort(Message income, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = income.getBookingID();
        if(relatedRequest== null) {
            logger.trace("2PC already finished, throw away message: " + income.toString());
            return answer;
        }

        if(messageFromCarBroker(income.getSenderAddress(), income.getSenderPort())) {
            if(relatedRequest.getCarBrokerState().equals(StatusTypes.ABORT)) {
                logger.trace("Already had that information, throw away message: " + income.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### CARBROKER MESSAGE ABORT!");
            msgHandler.updateRequestAtList(bookingId, StatusTypes.ABORT, null, null);
            logger.info("### RESULT => ROLLBACK!");
            relatedRequest.resetMessageCounter();
            msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
            Message answerForHotelBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
            answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
        }
        if(messageFromHotelBroker(income.getSenderAddress(), income.getSenderPort()) || relatedRequest.getGlobalState().equals(StatusTypes.ROLLBACK)) {
            if(relatedRequest.getHotelBrokerState().equals(StatusTypes.ABORT) || relatedRequest.getGlobalState().equals(StatusTypes.ROLLBACK)) {
                logger.trace("Already had that information, throw away message: " + income.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(income.getBookingID(), new Date());
            logger.info("### HOTELBROKER MESSAGE ABORT!");
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.ABORT, null);
            logger.info("### RESULT => ROLLBACK!");
            relatedRequest.resetMessageCounter();
            msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
            Message answerForCarBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
            answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
        }
        return answer;
    }

    public Message handleInquire(Message income, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = income.getBookingID();
        if(relatedRequest == null) {
            answer = msgFactory.buildThrowaway(bookingId, "Throwaway", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            return answer;
        }
        msgHandler.updateRequestTimestamp(bookingId, new Date());
        //resend COMMIT
        if(relatedRequest.getGlobalState().equals(StatusTypes.COMMIT)) {
            answer = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            return answer;
        }
        //resend ROLLBACK
        if(relatedRequest.getGlobalState().equals(StatusTypes.ROLLBACK)) {
            answer = msgFactory.buildRollback(bookingId, relatedRequest.contentToString(), msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            return answer;
        }
        //resend PREPARE
        if(relatedRequest.getCarBrokerState().equals(StatusTypes.ABORT) || relatedRequest.getHotelBrokerState().equals(StatusTypes.ABORT) || relatedRequest.getCarBrokerState().equals(StatusTypes.INITIALIZED) || relatedRequest.getHotelBrokerState().equals(StatusTypes.INITIALIZED)) {
            if(messageFromCarBroker(income.getSenderAddress(), income.getSenderPort())) {
                answer = msgFactory.buildPrepare(bookingId, relatedRequest.contentToString(), msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                return answer;
            }
            if(messageFromHotelBroker(income.getSenderAddress(), income.getSenderPort())) {
                answer = msgFactory.buildPrepare(bookingId, relatedRequest.contentToString(), msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                return answer;
            }
        }
        return answer;
    }

    public Message handleAcknowledge(Message income, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = income.getBookingID();
        if(relatedRequest == null) {
            logger.trace("2PC already finished, throw away message: " + income.toString());
            return answer;
        }
        relatedRequest.resetInquireCounter();
        if(messageFromCarBroker(income.getSenderAddress(), income.getSenderPort())) {
            msgHandler.updateRequestAtList(bookingId, StatusTypes.ACKNOWLEDGMENT, null, null);
            if(relatedRequest.bothAcknowledged()) {
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ACKNOWLEDGMENT);
                relatedRequest.resetMessageCounter();
                msgHandler.removeRequestFromList(bookingId);
                logger.info("### FINISHED 2PC");
            }
        }
        if(messageFromHotelBroker(income.getSenderAddress(), income.getSenderPort())) {
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.ACKNOWLEDGMENT, null);
            if(relatedRequest.bothAcknowledged()) {
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ACKNOWLEDGMENT);
                relatedRequest.resetMessageCounter();
                msgHandler.removeRequestFromList(bookingId);
                logger.info("### FINISHED 2PC");
            }
        }
        return answer;
    }

    private boolean messageFromCarBroker(InetAddress pAddress, int pPort) {
        boolean result = false;
        if(pAddress.equals(msgHandler.getServer().getCarBroker().getAddress()) && pPort == msgHandler.getServer().getCarBroker().getPort()) {
            result = true;
        }
        return result;
    }

    private boolean messageFromHotelBroker(InetAddress pAddress, int pPort) {
        boolean result = false;
        if(pAddress.equals(msgHandler.getServer().getHotelBroker().getAddress()) && pPort == msgHandler.getServer().getHotelBroker().getPort()) {
            result = true;
        }
        return result;
    }
}
