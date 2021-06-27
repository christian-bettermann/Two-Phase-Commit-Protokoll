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
        boolean free = checkRoomIsFreeInTimeZone(startTime);
        if(free) {
            bookRoom(startTime, EndTime);
        }
        return free;
    }

    private boolean checkRoomIsFreeInTimeZone(Date startTime) {
        boolean free;
        int actuallyReservationAmount = this.reservationList.size();
        int ctr = 0;
        if(actuallyReservationAmount >= 1) {
            for(int i = 0; i < actuallyReservationAmount; i++) {
                if(reservationList.get(i).getEndTime().before(startTime)) {
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

    public void bookRoom(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    public boolean removeBooking(Date startTime, Date EndTime) {
        boolean success = false;
        for(int i = 0; i < this.reservationList.size(); i++) {
            if(this.reservationList.get(i).getStartTime().equals(startTime) && this.reservationList.get(i).getEndTime().equals(EndTime)) {
                this.reservationList.remove(i);
                success = true;
                break;
            }
        }
        return success;
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
