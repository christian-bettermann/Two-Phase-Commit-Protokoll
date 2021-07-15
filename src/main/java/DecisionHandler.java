import Message.*;
import Request.ServerRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.Date;

public class DecisionHandler {
    //Attribute
    private ServerMessageHandler msgHandler;								//ServerMessageHandler that started the DecisionHandler
    private MessageFactory msgFactory;										//message builder
    private static final Logger logger = LogManager.getRootLogger();		//shared logger

    /**
	 * A constructor to get an answer for an incoming network package
	 * @param 	pHandler: 		ServerMessageHandler that started the DecisionHandler
	 * 			pMsgFactory: 	message builder
	 */
    public DecisionHandler(ServerMessageHandler pHandler, MessageFactory pMsgFactory) {
        this.msgHandler = pHandler;
        this.msgFactory = pMsgFactory;
    }

    /**
	 * A method to handle a ready message of a broker
	 * @param 	msgIn: 		message to answer on
	 * 			relatedRequest: 	request regarding the message
	 */
    public Message handleReady(Message msgIn, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = msgIn.getBookingID();
        //only proceed id the request is still open
        if (relatedRequest == null) {
            logger.trace("2PC already finished, throw away message: " + msgIn.toString());
            return answer;
        }
        //only proceed id the information in the message is new
        if (messageFromCarBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
            if (relatedRequest.getCarBrokerState().equals(StatusTypes.READY)) {
                logger.trace("Already had that information, throw away message: " + msgIn.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### CARBROKER MESSAGE READY!");
            //write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, StatusTypes.READY, null, null);
            if (relatedRequest.bothReady()) {
                logger.info("### RESULT => COMMIT!");
                relatedRequest.resetMessageCounter();
                
                //write new globalState to local object and stable store
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.COMMIT);
                //send commit to brokers
                Message answerForHotelBroker = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
                answer = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            } else if (relatedRequest.getHotelBrokerState().equals(StatusTypes.ABORT) && relatedRequest.getMessageCounter() >= 2) {
                logger.info("### RESULT => ROLLBACK!");
                relatedRequest.resetMessageCounter();
                
                //write new globalState to local object and stable store
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
                //send rollback to brokers
                Message answerForHotelBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
                answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            }
        }
        if(messageFromHotelBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
        	//only proceed id the information in the message is new
            if(relatedRequest.getHotelBrokerState().equals(StatusTypes.READY)) {
                logger.trace("Already had that information, throw away message: " + msgIn.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### HOTELBROKER MESSAGE READY!");
            //write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.READY, null);
            if(relatedRequest.bothReady()) {
                logger.info("### RESULT => COMMIT!");
                relatedRequest.resetMessageCounter();
                
                //write new globalState to local object and stable store
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.COMMIT);
                //send commit to brokers
                Message answerForCarBroker = msgFactory.buildCommit(msgIn.getBookingID(), "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
                answer = msgFactory.buildCommit(bookingId, "OkThenBook", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            } else if(relatedRequest.getCarBrokerState().equals(StatusTypes.ABORT) && relatedRequest.getMessageCounter() >= 2) {
                logger.info("### RESULT => ROLLBACK!");
                relatedRequest.resetMessageCounter();
                
                //write new globalState to local object and stable store
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
                //send rollback to brokers
                Message answerForCarBroker = msgFactory.buildRollback(msgIn.getBookingID(), "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
                answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            }
        }
        return answer;
    }

    /**
	 * A method to handle an abort message of a broker
	 * @param 	msgIn: 		message to answer on
	 * 			relatedRequest: 	request regarding the message
	 */
    public Message handleAbort(Message msgIn, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = msgIn.getBookingID();
        //only proceed id the request is still open
        if(relatedRequest== null) {
            logger.trace("2PC already finished, throw away message: " + msgIn.toString());
            return answer;
        }
        //only proceed id the information in the message is new
        if(messageFromCarBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
            if(relatedRequest.getCarBrokerState().equals(StatusTypes.ABORT)) {
                logger.trace("Already had that information, throw away message: " + msgIn.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(bookingId, new Date());
            logger.info("### CARBROKER MESSAGE ABORT!");
            
         	//write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, StatusTypes.ABORT, null, null);
            logger.info("### RESULT => ROLLBACK!");
            relatedRequest.resetMessageCounter();
            
            //write new globalState to local object and stable store
            msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
            //send rollback to brokers
            Message answerForHotelBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            msgHandler.answerParticipant(answerForHotelBroker, msgHandler.getServer().getHotelBroker().getAddress(), msgHandler.getServer().getHotelBroker().getPort());
            answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
        }
        if(messageFromHotelBroker(msgIn.getSenderAddress(), msgIn.getSenderPort()) || relatedRequest.getGlobalState().equals(StatusTypes.ROLLBACK)) {
        	//only proceed id the information in the message is new
        	if(relatedRequest.getHotelBrokerState().equals(StatusTypes.ABORT) || relatedRequest.getGlobalState().equals(StatusTypes.ROLLBACK)) {
                logger.trace("Already had that information, throw away message: " + msgIn.toString());
                return answer;
            }
            msgHandler.updateRequestTimestamp(msgIn.getBookingID(), new Date());
            logger.info("### HOTELBROKER MESSAGE ABORT!");
            
            //write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.ABORT, null);
            logger.info("### RESULT => ROLLBACK!");
            relatedRequest.resetMessageCounter();
            
            //write new globalState to local object and stable store
            msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ROLLBACK);
            //send rollback to brokers
            Message answerForCarBroker = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
            msgHandler.answerParticipant(answerForCarBroker, msgHandler.getServer().getCarBroker().getAddress(), msgHandler.getServer().getCarBroker().getPort());
            answer = msgFactory.buildRollback(bookingId, "OkThenRollback", msgHandler.getLocalAddress(), msgHandler.getLocalPort());
        }
        return answer;
    }

    /**
	 * A method to handle an inquire message of a broker
	 * @param 	msgIn: 		message to answer on
	 * 			relatedRequest: 	request regarding the message
	 */
    public Message handleInquire(Message msgIn, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = msgIn.getBookingID();
      //only proceed id the request is still open
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
            if(messageFromCarBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
                answer = msgFactory.buildPrepare(bookingId, relatedRequest.contentToString(), msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                return answer;
            }
            if(messageFromHotelBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
                answer = msgFactory.buildPrepare(bookingId, relatedRequest.contentToString(), msgHandler.getLocalAddress(), msgHandler.getLocalPort());
                return answer;
            }
        }
        return answer;
    }

    /**
	 * A method to handle an acknowledgement message of a broker
	 * @param 	msgIn: 		message to answer on
	 * 			relatedRequest: 	request regarding the message
	 */
    public Message handleAcknowledge(Message msgIn, ServerRequest relatedRequest) {
        Message answer = null;
        String bookingId = msgIn.getBookingID();
     	//only proceed id the request is still open
        if(relatedRequest == null) {
            logger.trace("2PC already finished, throw away message: " + msgIn.toString());
            return answer;
        }
        relatedRequest.resetInquireCounter();
        if(messageFromCarBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
        	//write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, StatusTypes.ACKNOWLEDGMENT, null, null);
            if(relatedRequest.bothAcknowledged()) {
            	//write new globalState to local object and stable store if both brokers acknowledged
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ACKNOWLEDGMENT);
                relatedRequest.resetMessageCounter();
                //remove finished request from local object and stable store
                msgHandler.removeRequestFromList(bookingId);
                logger.info("### FINISHED 2PC");
            }
        }
        if(messageFromHotelBroker(msgIn.getSenderAddress(), msgIn.getSenderPort())) {
        	//write new state to local object and stable store
            msgHandler.updateRequestAtList(bookingId, null, StatusTypes.ACKNOWLEDGMENT, null);
            if(relatedRequest.bothAcknowledged()) {
            	//write new globalState to local object and stable store if both brokers acknowledged
                msgHandler.updateRequestAtList(bookingId, null, null, StatusTypes.ACKNOWLEDGMENT);
                relatedRequest.resetMessageCounter();
                //remove finished request from local object and stable store
                msgHandler.removeRequestFromList(bookingId);
                logger.info("### FINISHED 2PC");
            }
        }
        return answer;
    }

    /**
	 * A method to check if an address and port matches with the data of the carBroker
	 * @param	pAddress: 	the address you want to check
	 * 			pPort:		the port you want to check
	 * @return	result:		true if it matches with the carBroker data, otherwise false
	 */
    private boolean messageFromCarBroker(InetAddress pAddress, int pPort) {
        boolean result = false;
        if(pAddress.equals(msgHandler.getServer().getCarBroker().getAddress()) && pPort == msgHandler.getServer().getCarBroker().getPort()) {
            result = true;
        }
        return result;
    }

	/**
	 * A method to check if an address and port matches with the data of the hotelBroker
	 * @param	pAddress: 	the address you want to check
	 * 			pPort:		the port you want to check
	 * @return	result:		true if it matches with the hotelBroker data, otherwise false
	 */
    private boolean messageFromHotelBroker(InetAddress pAddress, int pPort) {
        boolean result = false;
        if(pAddress.equals(msgHandler.getServer().getHotelBroker().getAddress()) && pPort == msgHandler.getServer().getHotelBroker().getPort()) {
            result = true;
        }
        return result;
    }
}
