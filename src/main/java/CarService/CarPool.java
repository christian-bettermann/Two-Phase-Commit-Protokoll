package CarService;

import Request.Request;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class CarPool {
    //Attribute
    private String poolName;
    private ArrayList<Car> carList;
    private ArrayList<Request> requestList;

    public CarPool(String pName) {
        this.poolName = pName;
        this.carList = new ArrayList<>();
        this.requestList = new ArrayList<Request>();
    }

    public boolean checkCarOfId(int bookingId, int carId, Date startTime, Date endTime) {
        boolean result = this.carList.get(carId).checkAndBookIfFree(startTime, endTime);
        if(result) {
            this.requestList.add(new Request(bookingId, startTime, endTime));
        }
        return result;
    }

    public void commitRequestOfBookingID(int bookingID) {

    }

    public void roolbackRequestOfBookingID(int bookingID) {

    }

    public void initialize() {
        JSONParser jParser = new JSONParser();
        try (FileReader reader = new FileReader("data.json"))
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
                        CarTypes.valueOf(carInfo.get("STATION_WAGON").toString())
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
}
