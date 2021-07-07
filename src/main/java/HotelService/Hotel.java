package HotelService;

import Message.StatusTypes;
import Request.RoomRequest;
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
    private ArrayList<Room> roomList;
    private ArrayList<RoomRequest> requestList;

    public Hotel() {
        this.roomList = new ArrayList<Room>();
        this.requestList = new ArrayList<RoomRequest>();
    }

    public boolean checkRoomOfId(InetAddress target, int port, String bookingId, int roomId, Date startTime, Date endTime) {
        boolean result = this.roomList.get(roomId - 1).checkAndBookIfFree(startTime, endTime);
        this.addRequestToList(target, port, bookingId, roomId, startTime, endTime, result);
        return result;
    }

    public void commitRequestOfBookingID(String bookingId) {
        RoomRequest request = getRequest(bookingId);
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/HotelService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject roomData = (JSONObject) jsonContent;
            Object roomDataContent = roomData.get("rooms");
            JSONArray roomJsonArray = (JSONArray) roomDataContent;
            Object singleRoomObject = roomJsonArray.get(request.getRoomId() - 1);
            JSONObject singleRoom = (JSONObject) singleRoomObject;
            Object reservationsObject = singleRoom.get("Reservations");
            JSONArray reservations = (JSONArray) reservationsObject;
            JSONObject reservation = new JSONObject();
            reservation.put("Id", request.getId());
            reservation.put("StartTime", request.getStartTime().getTime());
            reservation.put("EndTime", request.getEndTime().getTime());
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
        this.removeRequestFromList(bookingId);
    }

    public void roolbackRequestOfBookingID(String bookingId) {
        RoomRequest request = getRequest(bookingId);
        roomList.get(request.getRoomId() - 1).removeBooking(request.getStartTime(), request.getEndTime());
        this.removeRequestFromList(bookingId);
    }

    private RoomRequest getRequest(String bookingId) {
        RoomRequest request = null;
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getIdAsString().equals(bookingId)) {
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
                Room singleRoom = new Room(Integer.parseInt(roomInfo.get("Id").toString()),
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
                    singleRoom.bookRoom(new Date(Long.parseLong(startTime)), new Date(Long.parseLong(endTime)));
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
        try (FileReader reader = new FileReader("src/main/resources/HotelService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object roomRequestDataContent = requestsData.get("RoomRequests");
            JSONArray requests = (JSONArray) roomRequestDataContent;
            for (int i = 0; i < requests.size(); i++) {
                Object singleRoomRequestData = requests.get(i);
                JSONObject requestInfo = (JSONObject) singleRoomRequestData;
                RoomRequest singleRoomRequest = new RoomRequest(InetAddress.getByName(requestInfo.get("Target_IP").toString()),
                        Integer.parseInt(requestInfo.get("Target_Port").toString()),
                        requestInfo.get("BookingId").toString(),
                        Integer.parseInt(requestInfo.get("RoomId").toString()),
                        new Date(Long.parseLong(requestInfo.get("StartTime").toString())),
                        new Date(Long.parseLong(requestInfo.get("EndTime").toString())),
                        StatusTypes.valueOf(requestInfo.get("State").toString())
                );
                this.requestList.add(singleRoomRequest);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getInfoOfRooms() {
        String result = "";
        int lengthOfInfo = this.roomList.size();
        for(int i = 0; i < lengthOfInfo; i++) {
            if(i < lengthOfInfo - 1) {
                result = result + this.roomList.get(i).getInfo() + "!";
            } else {
                result = result + this.roomList.get(i).getInfo();
            }
        }
        return result;
    }

    public void addRequestToList(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime, boolean abortOrReady) {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/HotelService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("RoomRequests");
            JSONArray roomRequests = (JSONArray) carRequestDataContent;
            JSONObject roomRequest = new JSONObject();
            roomRequest.put("Target_IP", target.toString().replace("/", ""));
            roomRequest.put("Target_Port", port);
            roomRequest.put("BookingId", bookingId);
            roomRequest.put("RoomId", carId);
            roomRequest.put("StartTime", startTime.getTime());
            roomRequest.put("EndTime", endTime.getTime());
            if(abortOrReady) {
                this.requestList.add(new RoomRequest(target, port, bookingId, carId, startTime, endTime, StatusTypes.READY));
                roomRequest.put("State", StatusTypes.READY.toString());
            } else {
                this.requestList.add(new RoomRequest(target, port, bookingId, carId, startTime, endTime, StatusTypes.ABORT));
                roomRequest.put("State", StatusTypes.ABORT.toString());
            }
            roomRequests.add(roomRequest);
            try (FileWriter file = new FileWriter("src/main/resources/HotelService/requests.json")) {
                file.write(requestsData.toJSONString());
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

    public void removeRequestFromList(String bookingId) {
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getIdAsString().equals(bookingId)) {
                this.requestList.remove(i);
                break;
            }
        }
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/HotelService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("RoomRequests");
            JSONArray roomRequests = (JSONArray) carRequestDataContent;
            for(int i = 0; i < roomRequests.size(); i++) {
                Object requestData = roomRequests.get(i);
                JSONObject singleRequest = (JSONObject) requestData;
                if(singleRequest.get("BookingId").equals(bookingId)) {
                    roomRequests.remove(i);
                    break;
                }
            }
            try (FileWriter file = new FileWriter("src/main/resources/HotelService/requests.json")) {
                file.write(requestsData.toJSONString());
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

    public ArrayList<RoomRequest> getRequests() {
        return this.requestList;
    }
}
