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

    /**
     * A method to check if a car is free and if it is free to book it for the specified time
     * @param startTime Specified start
     * @param endTime Specified end
     * @return The method returns true if the car is free and was booked and it returns false if it was not free
     */
    public boolean checkAndBookIfFree(Date startTime, Date endTime) {
        boolean free = checkCarIsFreeInTimeZone(startTime, endTime);
        if(free) {
            bookCar(startTime, endTime);
        }
        return free;
    }

    /**
     * A method to check if a car is free in a specified time zone
     * @param startTime specified startpoint
     * @param endTime specified endpoint
     * @return The method returns true if the car is free and false if it is not
     */
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

    /**
     * A method to book a car at an specified timezone
     * @param startTime specified startpoint
     * @param EndTime specified endpoint
     */
    public void bookCar(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    /**
     * A method to remove an booking of the car for an specified timezone
     * @param startTime specified startpoint
     * @param EndTime specified endpoint
     */
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
