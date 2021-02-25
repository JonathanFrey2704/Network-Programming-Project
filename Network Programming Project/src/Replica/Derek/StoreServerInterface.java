package store;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author DRC
 */
public interface StoreServerInterface {
    
    public String addItem(String managerID, String itemID, String itemName, int quantity, double price);

    public String removeItem(String managerID, String itemID, int quantity);

    public String[] listItemAvailability(String managerID);

    public String purchaseItem(String customerID, String itemID, String dateOfPurchase);

    public String[] findItem(String customerID, String itemName);

    public String returnItem(String customerID, String itemID, String dateOfReturn);

    public String exchangeItem(String customerID, String newItemID, String oldItemID, String dateOfExchange);

}
