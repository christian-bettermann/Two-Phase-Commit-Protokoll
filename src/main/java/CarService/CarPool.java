package CarService;

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
    private String poolName;
    private ArrayList<Car> carList;
    private ArrayList<CarRequest> requestList;

    public CarPool(String pName) {
        this.poolName = pName;
        this.carList = new ArrayList<>();
        this.requestList = new ArrayList<CarRequest>();
    }

    public boolean checkCarOfId(InetAddress target, int port, int bookingId, int carId, Date startTime, Date endTime) {
        boolean result = this.carList.get(carId).checkAndBookIfFree(startTime, endTime);
        if(result) {
            this.addRequestToList(target, port, bookingId, carId, startTime, endTime);
        }
        return result;
    }

    public void commitRequestOfBookingID(int bookingID) {
        CarRequest request = getRequest(bookingID);
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject carData = (JSONObject) jsonContent;
            Object roomDataContent = carData.get("cars");
            JSONArray carJsonArray = (JSONArray) roomDataContent;
            Object singleCarObject = carJsonArray.get(request.getCarId());
            JSONObject singleCar = (JSONObject) singleCarObject;
            Object reservationsObject = singleCar.get("Reservations");
            JSONArray reservations = (JSONArray) reservationsObject;
            JSONObject reservation = new JSONObject();
            reservation.put("StartTime", request.getStartTime().toString());
            reservation.put("EndTime", request.getEndTime().toString());
            reservations.add(reservation);
            try (FileWriter file = new FileWriter("src/main/resources/CarService/data.json")) {
                file.write(carData.toJSONString());
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
        removeRequestFromList(bookingID);
    }

    public void roolbackRequestOfBookingID(int bookingID) {
        CarRequest request = getRequest(bookingID);
        carList.get(request.getCarId()).removeBooking(request.getStartTime(), request.getEndTime());
        removeRequestFromList(bookingID);
    }

    private CarRequest getRequest(int bookingId) {
        CarRequest request = null;
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
        try (FileReader reader = new FileReader("src/main/resources/CarService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject carData = (JSONObject) jsonContent;
            Object carDataContent = carData.get("cars");
            JSONArray carJsonArray = (JSONArray) carDataContent;
            for (int i = 0; i < carJsonArray.size(); i++) {
                Object singleCarData = carJsonArray.get(i);
                JSONObject carInfo = (JSONObject) singleCarData;
                Car singleCar = new Car(carInfo.get("Manufacturer").toString(),
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
            Object carRequestDataContent = requestsData.get("Requests");
            JSONArray requests = (JSONArray) carRequestDataContent;
            for (int i = 0; i < requests.size(); i++) {
                Object singleCarRequestData = requests.get(i);
                JSONObject requestInfo = (JSONObject) singleCarRequestData;
                CarRequest singleCarRequest = new CarRequest(InetAddress.getByName(requestInfo.get("Target_IP").toString()),
                        Integer.parseInt(requestInfo.get("Target_Port").toString()),
                        Integer.parseInt(requestInfo.get("BookingId").toString()),
                        Integer.parseInt(requestInfo.get("CarId").toString()),
                        new Date(Long.parseLong(requestInfo.get("StartTime").toString())),
                        new Date(Long.parseLong(requestInfo.get("EndTime").toString()))
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

    public void addRequestToList(InetAddress target, int port, int bookingId, int carId, Date startTime, Date endTime) {
        this.requestList.add(new CarRequest(target, port, bookingId, carId, startTime, endTime));
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/requests.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject requestsData = (JSONObject) jsonContent;
            Object carRequestDataContent = requestsData.get("CarRequests");
            JSONArray carRequests = (JSONArray) carRequestDataContent;
            JSONObject carRequest = new JSONObject();
            carRequest.put("Target_IP", target.toString());
            carRequest.put("Target_Port", port);
            carRequest.put("BookingId", bookingId);
            carRequest.put("CarId", carId);
            carRequest.put("StartTime", startTime.getTime());
            carRequest.put("EndTime", endTime.getTime());
            carRequests.add(carRequest);
            try (FileWriter file = new FileWriter("src/main/resources/CarService/requests.json")) {
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

    public void removeRequestFromList(int bookingId) {
        for(int i = 0; i < requestList.size(); i++) {
            if(this.requestList.get(i).getId() == bookingId) {
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
                if(Integer.parseInt(singleRequest.get("BookingId").toString()) == bookingId) {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<CarRequest> getRequests() {
        return this.requestList;
    }
}
