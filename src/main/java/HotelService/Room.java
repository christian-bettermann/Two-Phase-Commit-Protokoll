package HotelService;

import Calender.BlockedTimeZone;

import java.util.ArrayList;
import java.util.Date;

public class Room {
    //Attribute
    private final int id;
    private final int numberOfBeds;
    private final BedTypes bedType;
    private final int numberOfBaths;
    private final RoomTypes roomType;
    private ArrayList<BlockedTimeZone> reservationList;

    public Room(int pId, int pNumberOfBeds, BedTypes pBedType, int pNumberOfBaths, RoomTypes pRoomType) {
        this.id = pId;
        this.numberOfBeds = pNumberOfBeds;
        this.bedType = pBedType;
        this.numberOfBaths = pNumberOfBaths;
        this.roomType = pRoomType;
        this.reservationList = new ArrayList<BlockedTimeZone>();
    }

    /**
     * A method to check a room is free and if it is free to book it for the specified time
     * @param startTime Specified start
     * @param endTime Specified end
     * @return The method returns true if the room is free and was booked and it returns false if it was not free
     */
    public boolean checkAndBookIfFree(Date startTime, Date endTime) {
        boolean free = checkRoomIsFreeInTimeZone(startTime, endTime);
        if(free) {
            bookRoom(startTime, endTime);
        }
        return free;
    }

    /**
     * A method to check a room is free in a specified time zone
     * @param startTime specified startpoint
     * @param endTime specified endpoint
     * @return The method returns true if the room is free and false if it is not
     */
    private boolean checkRoomIsFreeInTimeZone(Date startTime, Date endTime) {
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
     * A method to book a room at an specified timezone
     * @param startTime specified startpoint
     * @param EndTime specified endpoint
     */
    public void bookRoom(Date startTime, Date EndTime) {
        this.reservationList.add(new BlockedTimeZone(startTime, EndTime));
    }

    /**
     * A method to remove an booking of the room for an specified timezone
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

    public String getInfo() {
        return this.id + "_" + this.numberOfBeds + "_" + this.bedType + "_" + this.numberOfBaths + "_" + this.roomType;
    }
}
