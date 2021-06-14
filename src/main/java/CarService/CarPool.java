package CarService;

import java.util.ArrayList;

public class CarPool {
    //Attribute
    private String poolName;
    private ArrayList<Car> carList;

    public CarPool(String pName) {
        this.poolName = pName;
        this.carList = new ArrayList<>();
    }

    public int getCarAmount() {
        return this.carList.size();
    }
    public Car getCarOfIndex(int i) {
        return this.carList.get(i);
    }

    public String getName() {
        return this.poolName;
    }
}
