import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Message.Message;
import Message.StatusTypes;
import Request.ServerRequest;

public class ServerMessageHandlerTimeChecker implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();
	private ServerMessageHandler smh;
	
	public ServerMessageHandlerTimeChecker(ServerMessageHandler smh) {
		this.smh = smh;
	}
	
	public void run() {
		while(true) {
			ArrayList<ServerRequest> smhrl = new ArrayList<ServerRequest>(smh.getRequestList());
			Iterator<ServerRequest> smhrlit = smhrl.iterator();
			while(smhrlit.hasNext()) {
				ServerRequest req = smhrlit.next();
				if(req.getTimestamp().getTime()  + 20 * 1000 < new Date().getTime()) {
					try {
						req.increaseInquireCounter();
						if(req.getInquireCounter() >= 30) {
							smh.removeRequestFromList(req.getId());
						} else {
							logger.trace("InquireCounter: "+ req.getInquireCounter());
							if(req.getGlobalState() == StatusTypes.COMMIT) {
								//COMMIT
								if (req.getCarBrokerState() != StatusTypes.ACKNOWLEDGMENT) {
									Message prepareMsgCar = smh.msgFactory.buildCommit(req.getId(), "OkThanCommit", InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, smh.server.getCarBroker().getAddress(), smh.server.getCarBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) + ">");
									smh.socket.send(preparePacketCar);
								}
								if (req.getHotelBrokerState() != StatusTypes.ACKNOWLEDGMENT) {
									Message prepareMsgHotel = smh.msgFactory.buildCommit(req.getId(), "OkThanCommit", InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, smh.server.getHotelBroker().getAddress(), smh.server.getHotelBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) + ">");
									smh.socket.send(preparePacketHotel);
								}
							} else if(req.getGlobalState() == StatusTypes.ROLLBACK) {
								//ROLLBACK
								if (req.getCarBrokerState() != StatusTypes.ACKNOWLEDGMENT) {
									Message prepareMsgCar = smh.msgFactory.buildRollback(req.getId(), "OkThanRollback", InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, smh.server.getCarBroker().getAddress(), smh.server.getCarBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) + ">");
									smh.socket.send(preparePacketCar);
								}
								if (req.getHotelBrokerState() != StatusTypes.ACKNOWLEDGMENT) {
									Message prepareMsgHotel = smh.msgFactory.buildRollback(req.getId(), "OkThanRollback", InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, smh.server.getHotelBroker().getAddress(), smh.server.getHotelBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) + ">");
									smh.socket.send(preparePacketHotel);
								}
							} else {
								//INITIALIZED
								if (req.getCarBrokerState() == StatusTypes.INITIALIZED) {
									Message prepareMsgCar = smh.msgFactory.buildPrepare(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, smh.server.getCarBroker().getAddress(), smh.server.getCarBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) + ">");
									smh.socket.send(preparePacketCar);
								}
								if (req.getHotelBrokerState() == StatusTypes.INITIALIZED) {
									Message prepareMsgHotel = smh.msgFactory.buildPrepare(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, smh.server.getHotelBroker().getAddress(), smh.server.getHotelBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) + ">");
									smh.socket.send(preparePacketHotel);
								}
								//ROLLBACK
								if (req.getCarBrokerState() == StatusTypes.ABORT) {
									Message abortMsgCar = smh.msgFactory.buildRollback(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket abortPacketCar = new DatagramPacket(abortMsgCar.toString().getBytes(), abortMsgCar.toString().getBytes().length, smh.server.getCarBroker().getAddress(), smh.server.getCarBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(abortPacketCar.getData(), 0, abortPacketCar.getLength()) + ">");
									smh.socket.send(abortPacketCar);
								}
								if (req.getHotelBrokerState() == StatusTypes.ABORT) {
									Message abortMsgHotel = smh.msgFactory.buildRollback(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
									DatagramPacket abortPacketHotel = new DatagramPacket(abortMsgHotel.toString().getBytes(), abortMsgHotel.toString().getBytes().length, smh.server.getHotelBroker().getAddress(), smh.server.getHotelBroker().getPort());
									logger.trace("<" + smh.name + "> resent: <" + new String(abortPacketHotel.getData(), 0, abortPacketHotel.getLength()) + ">");
									smh.socket.send(abortPacketHotel);
								}
							}
						}
						smh.updateRequestTimestamp(req.getId(), new Date());
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
