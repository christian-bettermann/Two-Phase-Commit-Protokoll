package CarService;

import Calender.BlockedTimeZone;

import java.util.ArrayList;
import java.util.Date;

public class Car {
    //Attribute
    private String manufacturer;
    private String model;
    private int horsePower;
    private CarTypes type;
    private ArrayList<BlockedTimeZone> reservationList;

    public Car(String pManufacturer, String pModel, int pHP, CarTypes pType) {
        this.manufacturer = pManufacturer;
        this.model = pModel;
        this.horsePower = pHP;
        this.type = pType;
        this.reservationList = new ArrayList<BlockedTimeZone>();
    }

    public boolean checkAndBookIfFree(Date startTime, Date EndTime) {
        boolean free = checkCarIsFreeInTimeZone(startTime, EndTime);
        if(free) {
            bookCar(startTime, EndTime);
        }
        return free;
    }

    private boolean checkCarIsFreeInTimeZone(Date startTime, Date EndTime) {
        boolean free = false;
        return free;
    }

    private void bookCar(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public int getHorsePower() {
        return horsePower;
    }

    public CarTypes getType() {
        return type;
    }
}
