package HotelService;

import Request.Request;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;

public class Hotel {
    //Attribute
    private String hotelName;
    private ArrayList<Room> roomList;
    private ArrayList<Request> requestList;

    public Hotel(String pName) {
        this.hotelName = pName;
        this.roomList = new ArrayList<Room>();
        this.requestList = new ArrayList<Request>();
    }


    public boolean checkRoomOfId(int bookingId, int roomId, Date startTime, Date endTime) {
        boolean result = this.roomList.get(roomId).checkAndBookIfFree(startTime, endTime);
        if(result) {
            this.requestList.add(new Request(bookingId, roomId, startTime, endTime));
        }
        return result;
    }

    public void commitRequestOfBookingID(int bookingID) {
        Request request = getRequest(bookingID);
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/HotelService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject roomData = (JSONObject) jsonContent;
            Object roomDataContent = roomData.get("rooms");
            JSONArray roomJsonArray = (JSONArray) roomDataContent;
            Object singleRoomObject = roomJsonArray.get(request.getInterestId());
            JSONObject singleRoom = (JSONObject) singleRoomObject;
            Object reservationsObject = singleRoom.get("Reservations");
            JSONArray reservations = (JSONArray) reservationsObject;
            JSONObject reservation = new JSONObject();
            reservation.put("StartTime", request.getStartTime().toString());
            reservation.put("EndTime", request.getEndTime().toString());
            reservations.add(reservation);
            try (FileWriter file = new FileWriter("src/main/resources/HotelService/data.json")) {
                file.write(roomData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void roolbackRequestOfBookingID(int bookingID) {
        Request request = getRequest(bookingID);
        roomList.get(request.getInterestId()).removeBooking(request.getStartTime(), request.getEndTime());
    }

    private Request getRequest(int bookingId) {
        Request request = null;
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId() == bookingId) {
                request = this.requestList.get(i);
                break;
            }
        }
        return request;
    }

    public void initialize() {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/HotelService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject roomData = (JSONObject) jsonContent;
            Object roomDataContent = roomData.get("rooms");
            JSONArray roomJsonArray = (JSONArray) roomDataContent;
            for (int i = 0; i < roomJsonArray.size(); i++) {
                Object singleRoomData = roomJsonArray.get(i);
                JSONObject roomInfo = (JSONObject) singleRoomData;
                Room singleRoom = new Room(roomInfo.get("Id").toString(),
                        Integer.parseInt(roomInfo.get("Beds").toString()),
                        BedTypes.valueOf(roomInfo.get("BedType").toString()),
                        Integer.parseInt(roomInfo.get("Bath").toString()),
                        RoomTypes.valueOf(roomInfo.get("Type").toString())
                );
                Object reservationData = roomInfo.get("Reservations");
                JSONArray reservationJsonArray = (JSONArray) reservationData;
                for(int j = 0; j < reservationJsonArray.size(); j++) {
                    Object singleBooking = reservationJsonArray.get(j);
                    JSONObject singleBookingData = (JSONObject) singleBooking;
                    String startTime = singleBookingData.get("StartTime").toString();
                    String endTime = singleBookingData.get("EndTime").toString();
                    singleRoom.bookRoom(new Date(Integer.parseInt(startTime)), new Date(Integer.parseInt(endTime)));
                }
                this.roomList.add(singleRoom);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
