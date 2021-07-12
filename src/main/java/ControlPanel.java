import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import HotelService.HotelBroker;
import CarService.CarBroker;

public class ControlPanel implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();
	private Thread serverOneThread, serverTwoThread, carBrokerThread, hotelBrokerThread;
	private Server serverOne, serverTwo;
	private CarBroker carBroker;
	private HotelBroker hotelBroker;
	
	public ControlPanel(Server serverOne, Server serverTwo, CarBroker carBroker, HotelBroker hotelBroker, Thread serverOneThread, Thread serverTwoThread, Thread carBrokerThread, Thread hotelBrokerThread) {
		logger.info("Creating ControlPanel...");
		this.serverOne = serverOne;
		this.serverTwo = serverTwo;
		this.carBroker = carBroker;
		this.hotelBroker = hotelBroker;
		this.serverOneThread = serverOneThread;
		this.serverTwoThread = serverTwoThread;
		this.carBrokerThread = carBrokerThread;
		this.hotelBrokerThread = hotelBrokerThread;
	}
	
	public void run() {
		JFrame f = new JFrame(); 
		f.setTitle("ControlPanel");
		JLabel slone = new JLabel("ServerOne:");
		JLabel sltwo = new JLabel("ServerTwo:");
		JButton sone = new JButton("Shutdown");
		JButton stwo = new JButton("Shutdown");
		JLabel cb = new JLabel("CarBroker:");
		JLabel hb = new JLabel("HotelBroker:");
		JButton c = new JButton("Shutdown");
		JButton h = new JButton("Shutdown");
		
	    
	    cb.setBounds(50, 25, 90, 20);
	    hb.setBounds(200, 25, 90, 20);
	    c.setBounds(50, 50, 100, 40);	  
	    h.setBounds(200, 50, 100, 40);	
	    
	    slone.setBounds(350, 25, 90, 20);
	    sltwo.setBounds(500, 25, 90, 20);
	    sone.setBounds(350, 50, 100, 40);	  
	    stwo.setBounds(500, 50, 100, 40);	
	    
	    f.add(slone);
	    f.add(sltwo);
	    f.add(sone);
	    f.add(stwo);
	    
	    f.add(c);
	    f.add(h);
	    f.add(cb);
	    f.add(hb);
	       
	    f.setSize(650, 150);
	    f.setLayout(null); 
		
		f.setVisible(true);  
		
		c.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	logger.info("Action triggered: " + c.getText() + " CarBroker");
		    	if(c.getText().equals("Shutdown")) {
		    		c.setText("Start");
		    		
		    		carBrokerThread.stop();
		    		carBrokerThread = null;
		    		
		    		c.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		c.setEnabled(true);
		    		return;
		    	}
		    	if(c.getText().equals("Start")) {
		    		c.setText("Shutdown");
		    		
		    		carBroker.closeSocket(); //free port
		    		carBroker = new CarBroker();
		    		carBrokerThread = new Thread(carBroker);
		    		carBrokerThread.start();
		    		
		    		c.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		c.setEnabled(true);
		    		return;
		    	}
		    }
		});
		
		h.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	logger.info("Action triggered: " + h.getText() + " HotelBroker");
		    	if(h.getText().equals("Shutdown")) {
		    		h.setText("Start");
		    		
		    		hotelBrokerThread.stop();
		    		hotelBrokerThread = null;
		    		
		    		h.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		h.setEnabled(true);
		    		return;
		    	}
		    	if(h.getText().equals("Start")) {
		    		h.setText("Shutdown");
		    		
		    		hotelBroker.closeSocket(); //free port
		    		hotelBroker = new HotelBroker();
		    		hotelBrokerThread = new Thread(hotelBroker);
		    		hotelBrokerThread.start();
		    		
		    		h.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		h.setEnabled(true);
		    		return;
		    	}
		    }
		});
		
		sone.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	logger.info("Action triggered: " + sone.getText() + " ServerOne");
		    	if(sone.getText().equals("Shutdown")) {
		    		sone.setText("Start");
		    		
		    		serverOne.shutdownHandler();
		    		serverOneThread.stop();
		    		serverOneThread = null;
		    		
		    		sone.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		sone.setEnabled(true);
		    		return;
		    	}
		    	if(sone.getText().equals("Start")) {
		    		sone.setText("Shutdown");
		    		
		    		serverOne.closeSocket(); //free port
		    		serverOne = new Server(1);
		    		serverOneThread = new Thread(serverOne);
		    		serverOneThread.start();
		    		
		    		sone.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		sone.setEnabled(true);
		    		return;
		    	}
		    }
		});
		
		stwo.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	logger.info("Action triggered: " + stwo.getText() + " ServerTwo");
		    	if(stwo.getText().equals("Shutdown")) {
		    		stwo.setText("Start");
		    		
		    		serverTwo.shutdownHandler();
		    		serverTwoThread.stop();
		    		serverTwoThread = null;
		    		
		    		stwo.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		stwo.setEnabled(true);
		    		return;
		    	}
		    	if(stwo.getText().equals("Start")) {
		    		stwo.setText("Shutdown");
		    		
		    		serverTwo.closeSocket(); //free port
		    		serverTwo = new Server(2);
		    		serverTwoThread = new Thread(serverTwo);
		    		serverTwoThread.start();
		    		
		    		stwo.setEnabled(false);
		    		try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
		    		stwo.setEnabled(true);
		    		return;
		    	}
		    }
		});
	}
 
}
