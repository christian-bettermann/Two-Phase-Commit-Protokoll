import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.toedter.calendar.JDateChooser;

import Message.*;

//this class generates a UI to start a booking request
public class Client implements Runnable {
	private static final Logger logger = LogManager.getRootLogger();	//shared logger
	private InetAddress localAddress;									//own address
	private DatagramSocket socket;										//UDP socket
	private byte[] buffer = new byte[1024];								//buffer for datagram packages
	int localPort;														//own port
	private InetAddress serverAddress;									//serverAddress for initial information connection about offered cars and rooms
	int serverPort = 30800;												//serverPort for initial information connection about offered cars and rooms
	private JTextArea textArea;											//output panel for logs
	private ButtonGroup bg;												//buttonGroup of RadioButtons for server selection
	private Long timestampBookingSent;									//saves timestamp of the last booking that was sent to a server
	
	public Client(int clientPort) {
		logger.trace("Creating Client...");
		this.localPort = clientPort;
	}
	
	public void run() {
		logger.info("Starting Client on port <" + localPort + "> ...");
		timestampBookingSent = Long.MAX_VALUE;
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
		
		//building message to request offered cars and rooms from brokers (over server)
		Message getInfoMessage = new Message(StatusTypes.INFO, localAddress, socket.getLocalPort(), "0", localAddress +":"+ localPort);
    	DatagramPacket packet = new DatagramPacket(getInfoMessage.toString().getBytes(), getInfoMessage.toString().getBytes().length, serverAddress, serverPort);
	    try {
	    	//wait a little so everything is set up
	    	TimeUnit.SECONDS.sleep(2);
			socket.send(packet);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
	    //building UI
	    JComboBox<String> cars = new JComboBox<String>();    	//Dropdown with cars
	    JComboBox<String> rooms = new JComboBox<String>();  	//Dropdown with rooms
	    JSONArray carsJson = new JSONArray();
	    JSONArray roomsJson = new JSONArray();
	    int i = 0;
	    //receive the two messages (one for the room information and one for the car information)
	    while(i < 2) {
	    	buffer = new byte[1024];
	    	DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		    try {
				socket.setSoTimeout(120000);
			    socket.receive(response);

			    String responseString = new String(response.getData(), 0, response.getLength());
			    Message responseMessage = new Message(responseString);
			    
			    //handling the car information message
			    if(responseMessage.getStatus() == StatusTypes.INFOCARS) {
			    	i++;
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
			            //add item to dropdown
			            cars.addItem("#"+ info[0] +": "+ info[1] +" "+ info[2]);
			    	}
			    }
			    
			    //handling the room information message
			    if(responseMessage.getStatus() == StatusTypes.INFOROOMS) {
			    	i++;
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
			            	//add item to dropdown
			            	rooms.addItem("#"+ info[0] +": "+ info[1] +" "+ info[2] +" Beds "+ info[4] +" Room");
			            } else {
			            	//add item to dropdown
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
		f.setSize(610, 500);
	    f.setLayout(null); 
	    
		JLabel c = new JLabel("Car:");
		JLabel r = new JLabel("Room:");
	    	    
	    c.setBounds(50, 25, 225, 20);
	    r.setBounds(300, 25, 225, 20);
	    cars.setBounds(50, 50, 250, 20);    
	    rooms.setBounds(300, 50, 250, 20); 
	    f.add(c);
	    f.add(r);
	    f.add(cars);
	    f.add(rooms);
	    
		JButton b = new JButton("Send Booking");
		b.setBounds(50, 400, 510, 40);		          
		f.add(b); 

		//DateChooser
		JLabel st = new JLabel("Start date:");
		st.setBounds(160, 100, 190, 20);
		JDateChooser startDateChooser = new JDateChooser();
		startDateChooser.setLocale(Locale.GERMANY);
		startDateChooser.setBounds(260, 100, 190, 20);
		f.add(st);
		f.add(startDateChooser);
		startDateChooser.setVisible(true);
		st.setVisible(true);
		
		//DateChooser
		JLabel et = new JLabel("End date:");
		et.setBounds(160, 150, 190, 20);
		JDateChooser endDateChooser = new JDateChooser();
		endDateChooser.setLocale(Locale.GERMANY);
		endDateChooser.setBounds(260, 150, 190, 20);
		f.add(et);
		f.add(endDateChooser);
		et.setVisible(true);
		endDateChooser.setVisible(true);
		
		//ButtonGroup with RadioButtons to choose the server the client sends the request to
		bg = new ButtonGroup();
		JLabel bglabel = new JLabel("Connecting to:");
		bglabel.setBounds(160, 200, 100, 20);
		f.add(bglabel);
		JRadioButton rbsone = new JRadioButton("Server1", true);
		JRadioButton rbstwo = new JRadioButton("Server2", false);
		rbsone.setBounds(260, 200, 100, 20);
		rbstwo.setBounds(360, 200, 100, 20);
		bg.add(rbsone);
		bg.add(rbstwo);
		f.add(rbsone);
		f.add(rbstwo);
		
		//Output panel for logs
		textArea = new JTextArea();
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.OUT_BOTTOM);
		textArea.setBounds(50, 230, 510, 150);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setBounds(50, 230, 510, 150);
		f.add(scroll);
		
		f.setVisible(true);  
		
		//Listen for Button Press (= send booking request)
		b.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	String carID = String.valueOf(cars.getSelectedIndex() + 1);
				String hotelID = String.valueOf(rooms.getSelectedIndex() + 1);
				String startTime = String.valueOf(startDateChooser.getDate().getTime());
				String endTime = String.valueOf(endDateChooser.getDate().getTime());
				
				sendBooking(carID, hotelID, startTime, endTime);
		    }
		});
		
		//set up finished
		logger.info("======================================================================================");
		
		//start listening for messages from servers
		boolean online = true;
        while (online) {
        	try {
        		buffer = new byte[1024];
        		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
	            InetAddress address = dp.getAddress();
	            int port = dp.getPort();
	            //building message from received DatagramPacket
	            Message received = new Message(new String(dp.getData(), 0, dp.getLength()), address, port);
	            logger.info("Client received:	<"+ received.toString() +">");
	            //calculating the right response for this message
				Message response = this.analyzeAndGetResponse(received);
				if(response != null) {
					buffer = response.toString().getBytes();
					dp = new DatagramPacket(buffer, buffer.length, address, port);
					logger.trace("Client sent: <"+ new String(dp.getData(), 0, dp.getLength()) +">");
		            socket.send(dp);
				}
        	} catch (SocketTimeoutException e) {
	            //Timeout
		    	logger.trace("ClientSocket timeout (no message received)!");
		    	//if client sent a booking and did not get a response yet, print an info to the output panel
		    	if(timestampBookingSent < new Date().getTime()) {
		    		timestampBookingSent = Long.MAX_VALUE;
		    		textArea.append("=> Could not reach Server, InfoMessage: Timeout\n\n");
		    	}
	        }catch (IOException e) {
				e.printStackTrace();
			}
        }
        socket.close();
	}
	
	//calculating the right response for all valid message types that the client should receive
	private Message analyzeAndGetResponse(Message msg) {
		Message response = null;
		try {
			switch(msg.getStatus()) {
				case BOOKING:
					//the server received the booking request of the client and sends him the bookingId for his request back for information (bookingId is calculated by the server)
					textArea.append("=> Server RECEIVED Booking Request <BookingID: "+ msg.getBookingID() +", CarID: "+ msg.getStatusMessageCarId() +", RoomID: "+ msg.getStatusMessageRoomId() +", StartTime: "+ new Date(msg.getStatusMessageStartTime()).getDate() +"-"+ (new Date(msg.getStatusMessageStartTime()).getMonth() + 1) +"-"+ (new Date(msg.getStatusMessageStartTime()).getYear() + 1900) +", EndTime: "+ new Date(msg.getStatusMessageEndTime()).getDate() +"-"+ (new Date(msg.getStatusMessageEndTime()).getMonth() + 1) +"-"+ (new Date(msg.getStatusMessageEndTime()).getYear() + 1900) +">\n\n");
					//client does not need to answer on this
					response = null;
					break;
				case COMMIT:
					//the server decided to commit the booking request, the client is informed, that his request will be successfully committed => Request committed
					textArea.append("=> Server COMMITTED Booking Request <BookingID: "+ msg.getBookingID() +">\n\n");
					//reset timestamp for last booking send
					timestampBookingSent = Long.MAX_VALUE;
					//client does not need to answer on this
					response = null;
					break;
				case ROLLBACK:
					//the server decided to rollback the booking request, the client is informed, that his request will be rolled back by the brokers => Request denied
					textArea.append("=> Server DENIED Booking Request <BookingID: "+ msg.getBookingID() +", InfoMessage: "+ msg.getStatusMessage() +">\n\n");
					//reset timestamp for last booking send
					timestampBookingSent = Long.MAX_VALUE;
					//client does not need to answer on this
					response = null;
					break;
				case THROWAWAY:
					//the server decided to throw the request away because at least one broker is taking to long (may be down) => Request denied
					textArea.append("=> Server DENIED Booking Request <BookingID: "+ msg.getBookingID() +", InfoMessage: Timeout>\n\n");
					//reset timestamp for last booking send
					timestampBookingSent = Long.MAX_VALUE;
					//client does not need to answer on this
					response = null;
					break;
				case ERROR:
					//the server received an invalid booking request from the client (chosen times for booking request might be wrong)
					if(msg.getStatusMessage().equals("ERROR_Invalid_Booking")) {
						textArea.append("=> Server received an invalid Booking Request (check dates)\n\n");
					}
					//reset timestamp for last booking send
					timestampBookingSent = Long.MAX_VALUE;
					//client does not need to answer on this
					response = null;
					break;
				default:
					response = null;
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	
	//send a new booking request to the chosen server with all the information from the UI
	public void sendBooking(String carID, String hotelID, String startTime, String endTime) {
		setSelectedServerPort(bg);
		Message m = new Message(StatusTypes.BOOKING, localAddress, localPort, "0", buildStatusMessage(carID, hotelID, startTime, endTime));
		logger.info("#####################################################################################################################################");
        logger.info("Trying to book the trip from "+ new Date(m.getStatusMessageStartTime()) +" until "+ new Date(m.getStatusMessageEndTime()) +" with the car "+ carID +" and the hotel "+ hotelID +".");
        DatagramPacket booking = new DatagramPacket(m.toString().getBytes(), m.toString().getBytes().length, serverAddress, serverPort);
	    try {
			socket.send(booking);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    //setting the timestamp for last booking send (to print something to the output panel if the client does not receive a response from the server)
	    timestampBookingSent = new Date().getTime();
	}  

	//build a typical message with all the relevant information
	public static String buildStatusMessage(String carID, String hotelID, String start, String end) {
		return carID + "_" + hotelID + "_" + start + "_" + end;
	}
	
	//update the local port variable by searching for the selected RadioButton
	public void setSelectedServerPort(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
            	
            	if(button.getText().equals("Server1")) {
            		serverPort = 30800;
            	}
            	if(button.getText().equals("Server2")) {
            		serverPort = 30801;
            	}
            }
        }
    }
}
