package HotelService;

import Calender.BlockedTimeZone;
import CarService.CarTypes;

import java.util.ArrayList;

public class Hotel {
    //Attribute
    private String name;
    private ArrayList<Room> roomList;

    public Hotel(String pName) {
        this.name = pName;
        this.roomList = new ArrayList<Room>();
    }
}
