package HotelService;

import JsonUtility.JsonHandler;
import Message.StatusTypes;
import Request.CarRequest;
import Request.RoomRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;

public class Hotel {
    //Attribute
    private JsonHandler jsonHandler;
    private final String dataFilePath;
    private final String requestsFilePath;
    private final ArrayList<Room> roomList;
    private final ArrayList<RoomRequest> requestList;

    public Hotel() {
        this.dataFilePath = "src/main/resources/HotelService/data.json";
        this.requestsFilePath = "src/main/resources/HotelService/requests.json";
        this.roomList = new ArrayList<>();
        this.requestList = new ArrayList<>();
        this.jsonHandler = new JsonHandler();
    }

    public boolean checkRoomOfId(InetAddress target, int port, String bookingId, int roomId, Date startTime, Date endTime) {
        RoomRequest request = getRequest(bookingId);
        boolean result;
        if(request  == null) {
            result = this.roomList.get(roomId - 1).checkAndBookIfFree(startTime, endTime);
            this.addRequestToList(target, port, bookingId, roomId, startTime, endTime, result);
            return result;
        } else {
            StatusTypes state = request.getState();
            if(state.equals(StatusTypes.READY)) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }
    }

    public boolean inquireMessage(String bookingId) {
        boolean result = false;
        RoomRequest request = getRequest(bookingId);
        if(request != null) {
	        if(request.getState().equals(StatusTypes.READY)) {
	            result = true;
	        }
        }
        return result;
    }

    public void commitRequestOfBookingID(String bookingId) {
        RoomRequest request = getRequest(bookingId);
        if(request != null) {
            JSONParser jParser = new JSONParser();
            try (FileReader reader = new FileReader(dataFilePath)) {
                JSONObject roomsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
                JSONArray rooms = jsonHandler.getAttributeAsJsonArray(roomsData.get("rooms"));
                JSONObject singleRoom = jsonHandler.getAttributeAsJsonObject(rooms.get(request.getRoomId() - 1));
                JSONArray reservations = jsonHandler.getAttributeAsJsonArray(singleRoom.get("Reservations"));
                JSONObject reservation = new JSONObject();
                reservation.put("Id", request.getId());
                reservation.put("StartTime", request.getStartTime().getTime());
                reservation.put("EndTime", request.getEndTime().getTime());
                reservations.add(reservation);
                try (FileWriter file = new FileWriter(dataFilePath)) {
                    file.write(roomsData.toJSONString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
            this.removeRequestFromList(bookingId);
        }
    }

    public void rollbackRequestOfBookingID(String bookingId) {
        RoomRequest request = getRequest(bookingId);
        if(request != null) {
            if (request.getState().equals(StatusTypes.READY)) {
                roomList.get(request.getRoomId() - 1).removeBooking(request.getStartTime(), request.getEndTime());
            }
            this.removeRequestFromList(bookingId);
        }
    }

    private RoomRequest getRequest(String bookingId) {
        RoomRequest request = null;
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId().equals(bookingId)) {
                request = this.requestList.get(i);
                break;
            }
        }
        return request;
    }

    public void initialize() {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(dataFilePath))
        {
            JSONObject roomsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray rooms = jsonHandler.getAttributeAsJsonArray(roomsData.get("rooms"));
            for (int i = 0; i < rooms.size(); i++) {
                JSONObject roomInfo = jsonHandler.getAttributeAsJsonObject(rooms.get(i));
                Room singleRoom = new Room(Integer.parseInt(roomInfo.get("Id").toString()),
                        Integer.parseInt(roomInfo.get("Beds").toString()),
                        BedTypes.valueOf(roomInfo.get("BedType").toString()),
                        Integer.parseInt(roomInfo.get("Bath").toString()),
                        RoomTypes.valueOf(roomInfo.get("Type").toString())
                );
                JSONArray reservationJsonArray = jsonHandler.getAttributeAsJsonArray(roomInfo.get("Reservations"));
                for(int j = 0; j < reservationJsonArray.size(); j++) {
                    JSONObject singleBookingData = jsonHandler.getAttributeAsJsonObject(reservationJsonArray.get(j));
                    String startTime = singleBookingData.get("StartTime").toString();
                    String endTime = singleBookingData.get("EndTime").toString();
                    singleRoom.bookRoom(new Date(Long.parseLong(startTime)), new Date(Long.parseLong(endTime)));
                }
                this.roomList.add(singleRoom);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray requests = jsonHandler.getAttributeAsJsonArray(requestsData.get("RoomRequests"));
            for (int i = 0; i < requests.size(); i++) {
                JSONObject requestInfo = jsonHandler.getAttributeAsJsonObject(requests.get(i));
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
        } catch (ParseException | IOException e) {
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
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray roomRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("RoomRequests"));
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
            try (FileWriter file = new FileWriter(requestsFilePath)) {
                file.write(requestsData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void removeRequestFromList(String bookingId) {
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId().equals(bookingId)) {
                this.requestList.remove(i);
                break;
            }
        }
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray roomRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("RoomRequests"));
            for(int i = 0; i < roomRequests.size(); i++) {
                JSONObject singleRequest = jsonHandler.getAttributeAsJsonObject(roomRequests.get(i));
                if(singleRequest.get("BookingId").equals(bookingId)) {
                    roomRequests.remove(i);
                    break;
                }
            }
            try (FileWriter file = new FileWriter(requestsFilePath)) {
                file.write(requestsData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void undoEverything(String bookingId) {
       RoomRequest request = getRequest(bookingId);
        int carId = -1;
        Date startTime = null;
        Date endTime = null;
        if(request != null) {
            this.rollbackRequestOfBookingID(bookingId);
        } else {
            JSONParser jParser = new JSONParser();
            try (FileReader reader = new FileReader(dataFilePath))
            {
                JSONObject carsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
                JSONArray cars= jsonHandler.getAttributeAsJsonArray(carsData.get("rooms"));
                for(int i = 0; i < cars.size(); i++) {
                    JSONObject singleCar = jsonHandler.getAttributeAsJsonObject(cars.get(i));
                    JSONArray reservationJsonArray = jsonHandler.getAttributeAsJsonArray(singleCar.get("Reservations"));
                    for(int j = 0; j < reservationJsonArray.size(); j++) {
                        JSONObject singleBookingData = jsonHandler.getAttributeAsJsonObject(reservationJsonArray.get(j));
                        if (singleBookingData.get("Id").toString().equals(bookingId)) {
                            carId = Integer.parseInt(singleBookingData.get("Id").toString());
                            startTime = new Date(Long.parseLong(singleBookingData.get("StartTime").toString()));
                            endTime = new Date(Long.parseLong(singleBookingData.get("EndTime").toString()));
                            reservationJsonArray.remove(i);
                            break;
                        }
                    }
                }
                try (FileWriter file = new FileWriter(dataFilePath)) {
                    file.write(carsData.toJSONString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
            if(carId != - 1) {
                this.roomList.get(carId - 1).removeBooking(startTime, endTime);
            }
        }
    }

    public ArrayList<RoomRequest> getRequests() {
        return this.requestList;
    }
}
