import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import HotelService.HotelBroker;
import CarService.CarBroker;

public class ControlPanel implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();
	private Server serverOne, serverTwo;
	private CarBroker carBroker;
	private HotelBroker hotelBroker;
	
	public ControlPanel(Server serverOne, Server serverTwo, CarBroker carBroker, HotelBroker hotelBroker) {
		logger.info("Creating ControlPanel...");
		this.serverOne = serverOne;
		this.serverTwo = serverTwo;
		this.carBroker = carBroker;
		this.hotelBroker = hotelBroker;
	}
	
	public void run() {
		JFrame f = new JFrame();  
		JLabel cb = new JLabel("CarBroker:");
		JLabel hb = new JLabel("HotelBroker:");
		JButton c = new JButton("Shutdown");
		JButton h = new JButton("Shutdown");
		
	    
	    cb.setBounds(50, 25, 90, 20);
	    hb.setBounds(150, 25, 90, 20);
	    c.setBounds(50, 400, 190, 40);	  
	    h.setBounds(50, 400, 190, 40);	
	    
	    f.add(c);
	    f.add(h);
	    f.add(cb);
	    f.add(hb);
	       
	    f.setSize(300, 500);
	    f.setLayout(null); 
		
		f.setVisible(true);  
		
		c.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	if(c.getText().equals("Shutdown")) {
		    		c.setText("Start");
		    	}
		    	if(c.getText().equals("Start")) {
		    		h.setText("Shutdown");
		    	}
		    }
		});
		
		h.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	if(h.getText().equals("Shutdown")) {
		    		h.setText("Start");
		    	}
		    	if(h.getText().equals("Start")) {
		    		h.setText("Shutdown");
		    	}
		    }
		});
		
	}
 
}
