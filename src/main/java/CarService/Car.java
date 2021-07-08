package CarService;

import Calender.BlockedTimeZone;

import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Car {
    //Attribute
	private static final Logger logger = LogManager.getRootLogger();
	private final int id;
    private final String manufacturer;
    private final String model;
    private final int horsePower;
    private final CarTypes type;
    private ArrayList<BlockedTimeZone> reservationList;

    public Car(int pid, String pManufacturer, String pModel, int pHP, CarTypes pType) {
        this.id = pid;
    	this.manufacturer = pManufacturer;
        this.model = pModel;
        this.horsePower = pHP;
        this.type = pType;
        this.reservationList = new ArrayList<BlockedTimeZone>();
    }

    public boolean checkAndBookIfFree(Date startTime, Date endTime) {
        boolean free = checkCarIsFreeInTimeZone(startTime, endTime);
        if(free) {
            bookCar(startTime, endTime);
        }
        return free;
    }

    private boolean checkCarIsFreeInTimeZone(Date startTime, Date endTime) {
        boolean free;
        int actuallyReservationAmount = this.reservationList.size();
        int ctr = 0;
        if(actuallyReservationAmount >= 1) {
            for(int i = 0; i < actuallyReservationAmount; i++) {
                if(!(startTime.before(reservationList.get(i).getEndTime()) && reservationList.get(i).getStartTime().before(endTime))) {
                    ctr++;
                }
            }
            if(ctr == actuallyReservationAmount) {
                free = true;
            } else {
                free = false;
            }
        } else {
            free = true;
        }
        return free;
    }

    public void bookCar(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    public void removeBooking(Date startTime, Date EndTime) {
        for(int i = 0; i < this.reservationList.size(); i++) {
            if(this.reservationList.get(i).getStartTime().equals(startTime) && this.reservationList.get(i).getEndTime().equals(EndTime)) {
                this.reservationList.remove(i);
                break;
            }
        }
    }

    public int getId() {
        return id;
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

    public String getInfo() {
        return this.id + "_" + this.manufacturer + "_" + this.model + "_" + this.horsePower + "_" + this.type;
    }
}
