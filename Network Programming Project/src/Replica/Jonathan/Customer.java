package Replica.Jonathan;

import java.util.HashMap;

public class Customer {
    public String customerID;
    public String branchID;
    public int budget;

    public Customer(String clientID){
        this.customerID = clientID;
        this.branchID = clientID.substring(0,2);
        this.budget = 1000;
    }
    public void purchaseItem(int price){
        budget -= price;
    }
    public void returnItem(int price){ budget += price; }

}
