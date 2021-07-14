package HotelService;

import JsonUtility.JsonHandler;
import Message.StatusTypes;
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

    /**
     * A method to check an incomming request for an car if it is free or not at the specified timezone
     * @param target the ip address of the sender
     * @param port the port address of the sender
     * @param bookingId the booking of the transaction
     * @param roomId the specific room id
     * @param startTime specified startpoint
     * @param endTime specified endpoint
     * @return The method returns true if the room is bookable at the specified timezone and false if not
     */
    public boolean checkRoomOfId(InetAddress target, int port, String bookingId, int roomId, Date startTime, Date endTime) {
        RoomRequest request = getRequest(bookingId);
        boolean result;
        if(request  == null) {
            //if request does not exists add it to list and check the avalaibility
            result = this.roomList.get(roomId - 1).checkAndBookIfFree(startTime, endTime);
            this.addRequestToList(target, port, bookingId, roomId, startTime, endTime, result);
            return result;
        } else {
            //if request does already exist check the result which was evaluated before
            StatusTypes state = request.getState();
            if(state.equals(StatusTypes.READY)) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }
    }

    /**
     * The method checks if an transaction was rollbacked or committed
     * @param bookingId id of the transaction
     * @return It returns true if it was committed and false if it was rollbacked
     */
    public boolean inquireMessage(String bookingId) {
        boolean result = false;
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
                        result = true;
                        break;
                    }
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * The method saves an request for an room to the stable storage
     * @param bookingId id of the request
     */
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

    /**
     * A method to delete an request for an room
     * @param bookingId id of the request
     */
    public void rollbackRequestOfBookingID(String bookingId) {
        RoomRequest request = getRequest(bookingId);
        if(request != null) {
            if (request.getState().equals(StatusTypes.READY)) {
                roomList.get(request.getRoomId() - 1).removeBooking(request.getStartTime(), request.getEndTime());
            }
            this.removeRequestFromList(bookingId);
        }
    }

    /**
     * A method to get an specified request from the requestlist
     * @param bookingId id of the request
     * @return The method returns the reference to the request
     */
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

    /**
     * A method to initialize the carpool and read the information from teh data file
     */
    public void initialize() {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(dataFilePath))
        {
            //read room data
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
                //read already booked reservations for the specific room
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
            //read all open request from the request file and at it to the requestlist
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

    /**
     * A method to build an string of all rooms with their informations
     * @return The method returns a string which contains all rooms and their information
     */
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

    /**
     * A method to add an request to the service it synchronize the request cache and stable storage
     * @param target ip address of the sender
     * @param port port of the sender
     * @param bookingId booking of the transaction
     * @param carId the specific room id
     * @param startTime the startpoint of booking
     * @param endTime the endpoint of booking
     * @param abortOrReady the car is free or it is not represented by true or false
     */
    public void addRequestToList(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime, boolean abortOrReady) {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            //add request to stable storage
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
                //add request to cache
                roomRequest.put("State", StatusTypes.READY.toString());
            } else {
                this.requestList.add(new RoomRequest(target, port, bookingId, carId, startTime, endTime, StatusTypes.ABORT));
                //add request to cache
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

    /**
     * A method to remove an request from the service it synchronize the request cache and stable storage
     * @param bookingId the id of the request which should be removed
     */
    public void removeRequestFromList(String bookingId) {
        //remove request from cache
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId().equals(bookingId)) {
                this.requestList.remove(i);
                break;
            }
        }
        //remove request from stable storage
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

    /**
     * A method to make every step of an handled request non existend
     * @param bookingId the id of the request which should be destroyed
     */
    public void undoEverything(String bookingId) {
       RoomRequest request = getRequest(bookingId);
        int roomId = -1;
        Date startTime = null;
        Date endTime = null;
        if(request != null) {
            //remove request and booking from cache
            this.rollbackRequestOfBookingID(bookingId);
        } else {
            JSONParser jParser = new JSONParser();
            try (FileReader reader = new FileReader(dataFilePath))
            {
                //remove booking from stable storage if it exists
                JSONObject roomsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
                JSONArray rooms= jsonHandler.getAttributeAsJsonArray(roomsData.get("rooms"));
                for(int i = 0; i < rooms.size(); i++) {
                    JSONObject singleRoom = jsonHandler.getAttributeAsJsonObject(rooms.get(i));
                    JSONArray reservationJsonArray = jsonHandler.getAttributeAsJsonArray(singleRoom.get("Reservations"));
                    for(int j = 0; j < reservationJsonArray.size(); j++) {
                        JSONObject singleBookingData = jsonHandler.getAttributeAsJsonObject(reservationJsonArray.get(j));
                        if (singleBookingData.get("Id").toString().equals(bookingId)) {
                            roomId = i;
                            startTime = new Date(Long.parseLong(singleBookingData.get("StartTime").toString()));
                            endTime = new Date(Long.parseLong(singleBookingData.get("EndTime").toString()));
                            reservationJsonArray.remove(j);
                            break;
                        }
                    }
                }
                try (FileWriter file = new FileWriter(dataFilePath)) {
                    file.write(roomsData.toJSONString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
            if(roomId != -1) {
                //remove booking from cache if it exists
                this.roomList.get(roomId).removeBooking(startTime, endTime);
            }
        }
    }

    /**
     * A method to get all open request as an list
     */
    public ArrayList<RoomRequest> getRequests() {
        return this.requestList;
    }
}
