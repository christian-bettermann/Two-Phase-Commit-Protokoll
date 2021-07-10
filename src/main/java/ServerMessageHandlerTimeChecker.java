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
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while(smhrlit.hasNext()) {
				ServerRequest req = smhrlit.next();
				logger.error(req.getTimestamp().getTime() +"############################");
				if(req.getTimestamp().getTime()  + 20 * 1000 < new Date().getTime()) {
					try {
						if(req.getCarBrokerState() == StatusTypes.INITIALIZED) {
							Message prepareMsgCar = smh.msgFactory.buildPrepare(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
							DatagramPacket preparePacketCar = new DatagramPacket(prepareMsgCar.toString().getBytes(), prepareMsgCar.toString().getBytes().length, smh.server.getCarBroker().getAddress(), smh.server.getCarBroker().getPort());
							logger.trace("<" + smh.name + "> resent: <"+ new String(preparePacketCar.getData(), 0, preparePacketCar.getLength()) +">");
							smh.socket.send(preparePacketCar);
							smh.updateRequestTimestamp(req.getId(), new Date());
						}
						if(req.getHotelBrokerState() == StatusTypes.INITIALIZED) {
							Message prepareMsgHotel = smh.msgFactory.buildPrepare(req.getId(), req.contentToString(), InetAddress.getLocalHost(), smh.socket.getLocalPort());
							DatagramPacket preparePacketHotel = new DatagramPacket(prepareMsgHotel.toString().getBytes(), prepareMsgHotel.toString().getBytes().length, smh.server.getHotelBroker().getAddress(), smh.server.getHotelBroker().getPort());
							logger.trace("<" + smh.name + "> resent: <"+ new String(preparePacketHotel.getData(), 0, preparePacketHotel.getLength()) +">");
							smh.socket.send(preparePacketHotel);
							smh.updateRequestTimestamp(req.getId(), new Date());
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
