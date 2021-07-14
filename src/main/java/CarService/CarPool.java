package CarService;

import JsonUtility.JsonHandler;
import Message.StatusTypes;
import Request.CarRequest;
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

public class CarPool {
    //Attribute
    private final JsonHandler jsonHandler;
    private final String dataFilePath;
    private final String requestsFilePath;
    private final ArrayList<Car> carList;
    private final ArrayList<CarRequest> requestList;

    public CarPool() {
        this.carList = new ArrayList<>();
        this.dataFilePath = "src/main/resources/CarService/data.json";
        this.requestsFilePath = "src/main/resources/CarService/requests.json";
        this.requestList = new ArrayList<>();
        this.jsonHandler = new JsonHandler();
    }

    /**
     * A method to check an incomming request for an car if it is free or not at the specified timezone
     * @param target the ip address of the sender
     * @param port the port address of the sender
     * @param bookingId the booking of the transaction
     * @param carId the specific car id
     * @param startTime specified startpoint
     * @param endTime specified endpoint
     * @return The method returns true if the car is bookable at the specified timezone and false if not
     */
    public boolean checkCarOfId(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime) {
        CarRequest request = getRequest(bookingId);
        boolean result;
        if(request == null) {
            result = this.carList.get(carId - 1).checkAndBookIfFree(startTime, endTime);
            this.addRequestToList(target, port, bookingId, carId, startTime, endTime, result);
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
            JSONArray cars= jsonHandler.getAttributeAsJsonArray(carsData.get("cars"));
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
     * The method saves an request for an car to the stable storage
     * @param bookingID id of the request
     */
    public void commitRequestOfBookingID(String bookingID) {
        CarRequest request = getRequest(bookingID);
        if(request != null) {
            JSONParser jParser = new JSONParser();
            try (FileReader reader = new FileReader(dataFilePath)) {
                JSONObject carsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
                JSONArray cars = jsonHandler.getAttributeAsJsonArray(carsData.get("cars"));
                JSONObject singleCar = jsonHandler.getAttributeAsJsonObject(cars.get(request.getCarId() - 1));
                JSONArray reservations = jsonHandler.getAttributeAsJsonArray(singleCar.get("Reservations"));
                JSONObject reservation = new JSONObject();
                reservation.put("Id", request.getId());
                reservation.put("StartTime", request.getStartTime().getTime());
                reservation.put("EndTime", request.getEndTime().getTime());
                reservations.add(reservation);
                try (FileWriter file = new FileWriter(dataFilePath)) {
                    file.write(carsData.toJSONString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            removeRequestFromList(bookingID);
        }
    }

    /**
     * A method to delete an request for an car
     * @param bookingID id of the request
     */
    public void rollbackRequestOfBookingID(String bookingID) {
        CarRequest request = getRequest(bookingID);
        if(request != null) {
            if (request.getState().equals(StatusTypes.READY)) {
                carList.get(request.getCarId() - 1).removeBooking(request.getStartTime(), request.getEndTime());
            }
            this.removeRequestFromList(bookingID);
        }
    }

    /**
     * A method to get an specified request from the requestlist
     * @param bookingId id of the request
     * @return The method returns the reference to the request
     */
    private CarRequest getRequest(String bookingId) {
        CarRequest request = null;
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
            //read car data
            JSONObject carsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray cars= jsonHandler.getAttributeAsJsonArray(carsData.get("cars"));
            for (int i = 0; i < cars.size(); i++) {
                JSONObject carInfo = jsonHandler.getAttributeAsJsonObject(cars.get(i));
                Car singleCar = new Car(Integer.parseInt(carInfo.get("Id").toString()),carInfo.get("Manufacturer").toString(),
                        carInfo.get("Model").toString(),
                        Integer.parseInt(carInfo.get("HorsePower").toString()),
                        CarTypes.valueOf(carInfo.get("Type").toString())
                );
                //read already booked reservations for the specific car
                JSONArray reservationJsonArray = jsonHandler.getAttributeAsJsonArray(carInfo.get("Reservations"));
                for(int j = 0; j < reservationJsonArray.size(); j++) {
                    JSONObject singleBookingData = jsonHandler.getAttributeAsJsonObject(reservationJsonArray.get(j));
                    String startTime = singleBookingData.get("StartTime").toString();
                    String endTime = singleBookingData.get("EndTime").toString();
                    singleCar.bookCar(new Date(Long.parseLong(startTime)), new Date(Long.parseLong(endTime)));
                }
                this.carList.add(singleCar);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            //read all open request from the request file and at it to the requestlist
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray requests = jsonHandler.getAttributeAsJsonArray(requestsData.get("CarRequests"));
            for (int i = 0; i < requests.size(); i++) {
                JSONObject requestInfo = jsonHandler.getAttributeAsJsonObject(requests.get(i));
                CarRequest singleCarRequest = new CarRequest(InetAddress.getByName(requestInfo.get("Target_IP").toString()),
                        Integer.parseInt(requestInfo.get("Target_Port").toString()),
                        requestInfo.get("BookingId").toString(),
                        Integer.parseInt(requestInfo.get("CarId").toString()),
                        new Date(Long.parseLong(requestInfo.get("StartTime").toString())),
                        new Date(Long.parseLong(requestInfo.get("EndTime").toString())),
                        StatusTypes.valueOf(requestInfo.get("State").toString())
                );
                this.requestList.add(singleCarRequest);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method to build an string of all cars with their informations
     * @return The method returns a string which contains all cars and their information
     */
    public String getInfoOfCars() {
        String result = "";
        int lengthOfInfo = this.carList.size();
        for(int i = 0; i < lengthOfInfo; i++) {
            if(i < lengthOfInfo - 1) {
                result = result + this.carList.get(i).getInfo() + "!";
            } else {
                result = result + this.carList.get(i).getInfo();
            }
        }
        return result;
    }

    /**
     * A method to add an request to the service it synchronize the request cache and stable storage
     * @param target ip address of the sender
     * @param port port of the sender
     * @param bookingId booking of the transaction
     * @param carId the specific car id
     * @param startTime the startpoint of booking
     * @param endTime the endpoint of booking
     * @param abortOrReady the car is free or it is not represented by true or false
     */
    public void addRequestToList(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime, boolean abortOrReady) {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray carRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("CarRequests"));
            JSONObject carRequest = new JSONObject();
            carRequest.put("Target_IP", target.toString().replace("/", ""));
            carRequest.put("Target_Port", port);
            carRequest.put("BookingId", bookingId);
            carRequest.put("CarId", carId);
            carRequest.put("StartTime", startTime.getTime());
            carRequest.put("EndTime", endTime.getTime());
            if(abortOrReady) {
                carRequest.put("State", StatusTypes.READY.toString());
                this.requestList.add(new CarRequest(target, port, bookingId, carId, startTime, endTime, StatusTypes.READY));
            } else {
                carRequest.put("State", StatusTypes.ABORT.toString());
                this.requestList.add(new CarRequest(target, port, bookingId, carId, startTime, endTime, StatusTypes.ABORT));
            }
            carRequests.add(carRequest);
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
     * A method to remove an request from the service it snychronize the request cache and stable storage
     * @param bookingId the id of the request which should be removed
     */
    public void removeRequestFromList(String bookingId) {
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId().equals( bookingId)) {
                this.requestList.remove(i);
                break;
            }
        }
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader(requestsFilePath))
        {
            JSONObject requestsData = jsonHandler.getAttributeAsJsonObject(jParser.parse(reader));
            JSONArray carRequests = jsonHandler.getAttributeAsJsonArray(requestsData.get("CarRequests"));
            for(int i = 0; i < carRequests.size(); i++) {
                JSONObject singleRequest = jsonHandler.getAttributeAsJsonObject(carRequests.get(i));
                if(singleRequest.get("BookingId").toString().equals(bookingId)) {
                    carRequests.remove(i);
                    break;
                }
            }
            try (FileWriter file = new FileWriter(requestsFilePath)) {
                file.write(requestsData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method to make every step of an handled request non existend
     * @param bookingId the id of the request which should be destroyed
     */
    public void undoEverything(String bookingId) {
        CarRequest request = getRequest(bookingId);
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
                JSONArray cars= jsonHandler.getAttributeAsJsonArray(carsData.get("cars"));
                for(int i = 0; i < cars.size(); i++) {
                    JSONObject singleCar = jsonHandler.getAttributeAsJsonObject(cars.get(i));
                    JSONArray reservationJsonArray = jsonHandler.getAttributeAsJsonArray(singleCar.get("Reservations"));
                    for(int j = 0; j < reservationJsonArray.size(); j++) {
                        JSONObject singleBookingData = jsonHandler.getAttributeAsJsonObject(reservationJsonArray.get(j));
                        if (singleBookingData.get("Id").toString().equals(bookingId)) {
                            carId = i;
                            startTime = new Date(Long.parseLong(singleBookingData.get("StartTime").toString()));
                            endTime = new Date(Long.parseLong(singleBookingData.get("EndTime").toString()));
                            reservationJsonArray.remove(j);
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
            if(carId != -1) {
                this.carList.get(carId).removeBooking(startTime, endTime);
            }
        }
    }

    /**
     * A method to get all open request as an list
     */
    public ArrayList<CarRequest> getRequests() {
        return this.requestList;
    }
}
