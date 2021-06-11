package HotelService;

import Calender.BlockedTimeZone;

import java.util.ArrayList;
import java.util.Date;

public class Room {
    //Attribute
    private final String id;
    private final int numberOfBeds;
    private final BedTypes bedType;
    private final int numberOfBaths;
    private final RoomTypes roomType;
    private ArrayList<BlockedTimeZone> reservationList;

    public Room(String pId, int pNumberOfBeds, BedTypes pBedType, int pNumberOfBaths, RoomTypes pRoomType) {
        this.id = pId;
        this.numberOfBeds = pNumberOfBeds;
        this.bedType = pBedType;
        this.numberOfBaths = pNumberOfBaths;
        this.roomType = pRoomType;
        this.reservationList = new ArrayList<BlockedTimeZone>();
    }

    public boolean checkAndBookIfFree(Date startTime, Date EndTime) {
        boolean free = checkRoomIsFreeInTimeZone(startTime, EndTime);
        if(free) {
            bookRoom(startTime, EndTime);
        }
        return free;
    }

    private boolean checkRoomIsFreeInTimeZone(Date startTime, Date EndTime) {
        boolean free = false;
        return free;
    }

    private void bookRoom(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    public String getId() {
        return id;
    }

    public int getNumberOfBeds() {
        return numberOfBeds;
    }

    public BedTypes getBedType() {
        return bedType;
    }

    public int getNumberOfBaths() {
        return numberOfBaths;
    }

    public RoomTypes getRoomType() {
        return roomType;
    }
}
