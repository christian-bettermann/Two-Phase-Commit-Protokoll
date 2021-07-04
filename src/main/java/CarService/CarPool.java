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

    public boolean checkCarOfId(int bookingId, int carId, Date startTime, Date endTime) {
        boolean result = this.carList.get(carId).checkAndBookIfFree(startTime, endTime);
        if(result) {
            this.requestList.add(new CarRequest(bookingId, carId, startTime, endTime));
        }
        return result;
    }

    public void commitRequestOfBookingID(int bookingID) {
        CarRequest request = getRequest(bookingID);
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("src/main/resources/CarService/data.json"))
        {
            Object jsonContent = jParser.parse(reader);
            JSONObject roomData = (JSONObject) jsonContent;
            Object roomDataContent = roomData.get("cars");
            JSONArray roomJsonArray = (JSONArray) roomDataContent;
            Object singleRoomObject = roomJsonArray.get(request.getCarId());
            JSONObject singleRoom = (JSONObject) singleRoomObject;
            Object reservationsObject = singleRoom.get("Reservations");
            JSONArray reservations = (JSONArray) reservationsObject;
            JSONObject reservation = new JSONObject();
            reservation.put("StartTime", request.getStartTime().toString());
            reservation.put("EndTime", request.getEndTime().toString());
            reservations.add(reservation);
            try (FileWriter file = new FileWriter("src/main/resources/CarService/data.json")) {
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
        CarRequest request = getRequest(bookingID);
        carList.get(request.getCarId()).removeBooking(request.getStartTime(), request.getEndTime());
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
                    singleCar.bookCar(new Date(Integer.parseInt(startTime)), new Date(Integer.parseInt(endTime)));
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
}
