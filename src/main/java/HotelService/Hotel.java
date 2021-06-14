package HotelService;

import java.util.ArrayList;

public class Hotel {
    //Attribute
    private String hotelName;
    private ArrayList<Room> roomList;

    public Hotel(String pName) {
        this.hotelName = pName;
        this.roomList = new ArrayList<Room>();
    }

    public int getRoomAmount() {
        return this.roomList.size();
    }
    public Room getRoomOfIndex(int i) {
        return this.roomList.get(i);
    }

    public String getName() {
        return this.hotelName;
    }
}
