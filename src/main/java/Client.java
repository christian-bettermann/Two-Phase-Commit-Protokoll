import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.toedter.calendar.JDateChooser;

import Message.*;


public class Client implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();
	private InetAddress localAddress;
	private InetAddress serverAddress;
	private DatagramSocket socket;
	private byte[] buffer = new byte[1024];
	int localPort;
	int serverPort = 30800; //serverTwo 30801;
	
	public Client(int clientPort) {
		logger.trace("Creating Client...");
		this.localPort = clientPort;
	}
	
	public void run() {
		logger.info("Starting Client on port <" + localPort + "> ...");
		try {
			localAddress = InetAddress.getLocalHost();
			serverAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		try {
			socket = new DatagramSocket(localPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		Message getInfoMessage = new Message(StatusTypes.INFO, localAddress, socket.getLocalPort(), "0", localAddress +":"+ localPort);
    	DatagramPacket packet = new DatagramPacket(getInfoMessage.toString().getBytes(), getInfoMessage.toString().getBytes().length, serverAddress, serverPort);
	    try {
	    	TimeUnit.SECONDS.sleep(2);
			socket.send(packet);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
	    JComboBox<String> cars = new JComboBox<String>();    
	    JComboBox<String> rooms = new JComboBox<String>();  
	    JSONArray carsJson = new JSONArray();
	    JSONArray roomsJson = new JSONArray();
	    for(int i = 0; i < 2; i++) {
	    	buffer = new byte[1024];
	    	DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		    try {
				socket.setSoTimeout(120000);
			    socket.receive(response);

			    String responseString = new String(response.getData(), 0, response.getLength());
			    Message responseMessage = new Message(responseString);
			    
			    if(responseMessage.getStatus() == StatusTypes.INFOCARS) {
			    	//parse responseMessage.getStatusMessage() to JSON and fill dropdown	    	
			    	String[] infoCarsArray = responseMessage.getStatusMessage().split("!");
			    	for(String carString : infoCarsArray) {
			    		String[] info = carString.split("_");
				    	JSONObject car = new JSONObject();
				    	car.put("Id", info[0]);
			            car.put("Manufacturer", info[1]);
			            car.put("Model", info[2]);
			            car.put("HorsePower", info[3]);
			            car.put("Type", info[4]);
			            carsJson.add(car);
			            cars.addItem("#"+ info[0] +": "+ info[1] +" "+ info[2]);
			    	}
			    }
			    
			    if(responseMessage.getStatus() == StatusTypes.INFOROOMS) {
			    	//parse responseMessage.getStatusMessage() to JSON and fill dropdown
			    	String[] infoRoomsArray = responseMessage.getStatusMessage().split("!");
			    	for(String roomString : infoRoomsArray) {
			    		String[] info = roomString.split("_");
				    	JSONObject room = new JSONObject();
				    	room.put("Id", info[0]);
				    	room.put("Beds", info[1]);
			            room.put("BedType", info[2]);
			            room.put("Bath", info[3]);
			            room.put("Type", info[4]);
			            roomsJson.add(room);
			            if(Integer.parseInt(info[1]) > 1) {
			            	rooms.addItem("#"+ info[0] +": "+ info[1] +" "+ info[2] +" Beds "+ info[4] +" Room");
			            } else {
			            	rooms.addItem("#"+ info[0] +": "+ info[1] +" "+ info[2] +" Bed "+ info[4] +" Room");
			            }
			            
			    	}
			    }
		    } catch (SocketException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
	    }		

		JFrame f = new JFrame();
		f.setTitle("Client");
		
		JLabel c = new JLabel("Car:");
		JLabel r = new JLabel("Room:");
	    	    
	    c.setBounds(50, 25, 90, 20);
	    r.setBounds(150, 25, 90, 20);
	    cars.setBounds(50, 50, 90, 20);    
	    rooms.setBounds(150, 50, 190, 20); 
	    
	    f.add(c);
	    f.add(r);
	    f.add(cars);
	    f.add(rooms);
	       
	    f.setSize(390, 500);
	    f.setLayout(null); 
	    
		JButton b = new JButton("Send Booking");
		b.setBounds(50, 400, 290, 40);		          
		f.add(b); 

		JLabel st = new JLabel("Start date:");
		st.setBounds(50, 100, 190, 20);
		JDateChooser startDateChooser = new JDateChooser();
		startDateChooser.setLocale(Locale.GERMANY);
		startDateChooser.setBounds(150, 100, 190, 20);
		f.add(st);
		f.add(startDateChooser);
		startDateChooser.setVisible(true);
		st.setVisible(true);
		
		JLabel et = new JLabel("End date:");
		et.setBounds(50, 150, 190, 20);
		JDateChooser endDateChooser = new JDateChooser();
		endDateChooser.setLocale(Locale.GERMANY);
		endDateChooser.setBounds(150, 150, 190, 20);
		f.add(et);
		f.add(endDateChooser);
		et.setVisible(true);
		endDateChooser.setVisible(true);
		
		f.setVisible(true);  
		
		b.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	String carID = String.valueOf(cars.getSelectedIndex() + 1);
				String hotelID = String.valueOf(rooms.getSelectedIndex() + 1);
				String startTime = String.valueOf(startDateChooser.getDate().getTime());
				String endTime = String.valueOf(endDateChooser.getDate().getTime());
				
				sendBooking(carID, hotelID, startTime, endTime);
		    }
		});
		
		
		//start receiving from servers
		//handle BOOKING & ACKNOWLEDGMENT & ERROR for bookingRequest
		//####################################################
	}
	
	public void sendBooking(String carID, String hotelID, String startTime, String endTime) {
		Message m = new Message(StatusTypes.BOOKING, localAddress, localPort, "0", buildStatusMessage(carID, hotelID, startTime, endTime));
        logger.info("Trying to book the trip from "+ new Date(m.getStatusMessageStartTime()) +" until "+ new Date(m.getStatusMessageEndTime()) +" with the car "+ carID +" and the hotel "+ hotelID +".");
        DatagramPacket booking = new DatagramPacket(m.toString().getBytes(), m.toString().getBytes().length, serverAddress, serverPort);
	    try {
			socket.send(booking);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}  

	public static String buildStatusMessage(String carID, String hotelID, String start, String end) {
		return carID + "_" + hotelID + "_" + start + "_" + end;
	}
}
