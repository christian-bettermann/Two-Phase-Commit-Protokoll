package CarService;

import Message.StatusTypes;
import Request.CarRequest;
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

public class CarPool {
    //Attribute
    private final ArrayList<Car> carList;
    private final ArrayList<CarRequest> requestList;

    public CarPool() {
        this.carList = new ArrayList<Car>();
        this.requestList = new ArrayList<CarRequest>();
    }

    public boolean checkCarOfId(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime) {
        boolean result = this.carList.get(carId - 1).checkAndBookIfFree(startTime, endTime);
        this.addRequestToList(target, port, bookingId, carId, startTime, endTime, result);
        return result;
    }

    public void commitRequestOfBookingID(String bookingID) {
        CarRequest request = getRequest(bookingID);
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject carData = (JSONObject) jsonContent;
            Object roomDataContent = carData.get("cars");
            JSONArray carJsonArray = (JSONArray) roomDataContent;
            Object singleCarObject = carJsonArray.get(request.getCarId() - 1);
            JSONObject singleCar = (JSONObject) singleCarObject;
            Object reservationsObject = singleCar.get("Reservations");
            JSONArray reservations = (JSONArray) reservationsObject;
            JSONObject reservation = new JSONObject();
            reservation.put("Id", request.getId());
            reservation.put("StartTime", request.getStartTime().getTime());
            reservation.put("EndTime", request.getEndTime().getTime());
            reservations.add(reservation);
            try (FileWriter file = new FileWriter("src/main/resources/CarService/data.json")) {
                file.write(carData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        removeRequestFromList(bookingID);
    }

    public void roolbackRequestOfBookingID(String bookingID) {
        CarRequest request = getRequest(bookingID);
        carList.get(request.getCarId() - 1).removeBooking(request.getStartTime(), request.getEndTime());
        this.removeRequestFromList(bookingID);
    }

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


    public void initialize() {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject carData = (JSONObject) jsonContent;
            Object carDataContent = carData.get("cars");
            JSONArray carJsonArray = (JSONArray) carDataContent;
            for (int i = 0; i < carJsonArray.size(); i++) {
                Object singleCarData = carJsonArray.get(i);
                JSONObject carInfo = (JSONObject) singleCarData;
                Car singleCar = new Car(Integer.parseInt(carInfo.get("Id").toString()),carInfo.get("Manufacturer").toString(),
                        carInfo.get("Model").toString(),
                        Integer.parseInt(carInfo.get("HorsePower").toString()),
                        CarTypes.valueOf(carInfo.get("Type").toString())
                );
                Object reservationData = carInfo.get("Reservations");
                JSONArray reservationJsonArray = (JSONArray) reservationData;
                for(int j = 0; j < reservationJsonArray.size(); j++) {
                    Object singleBooking = reservationJsonArray.get(j);
                    JSONObject singleBookingData = (JSONObject) singleBooking;
                    String startTime = singleBookingData.get("StartTime").toString();
                    String endTime = singleBookingData.get("EndTime").toString();
                    singleCar.bookCar(new Date(Long.parseLong(startTime)), new Date(Long.parseLong(endTime)));
                }
                this.carList.add(singleCar);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try (FileReader reader = new FileReader("src/main/resources/CarService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("CarRequests");
            JSONArray requests = (JSONArray) carRequestDataContent;
            for (int i = 0; i < requests.size(); i++) {
                Object singleCarRequestData = requests.get(i);
                JSONObject requestInfo = (JSONObject) singleCarRequestData;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

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

    public void addRequestToList(InetAddress target, int port, String bookingId, int carId, Date startTime, Date endTime, boolean abortOrReady) {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("CarRequests");
            JSONArray carRequests = (JSONArray) carRequestDataContent;
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
            try (FileWriter file = new FileWriter("src/main/resources/CarService/requests.json")) {
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
            if(this.requestList.get(i).getId().equals( bookingId)) {
                this.requestList.remove(i);
                break;
            }
        }
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("CarRequests");
            JSONArray carRequests = (JSONArray) carRequestDataContent;
            for(int i = 0; i < carRequests.size(); i++) {
                Object requestData = carRequests.get(i);
                JSONObject singleRequest = (JSONObject) requestData;
                if(singleRequest.get("BookingId").toString().equals(bookingId)) {
                    carRequests.remove(i);
                    break;
                }
            }
            try (FileWriter file = new FileWriter("src/main/resources/CarService/requests.json")) {
                file.write(requestsData.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<CarRequest> getRequests() {
        return this.requestList;
    }
}
