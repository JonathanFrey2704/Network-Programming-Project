package Replica.Jonathan;

public class Item {
    String itemID;
    String name;
    int quantity;
    int price;

    public Item(String itemID, String name, int quantity2, int price2) {
        this.itemID = itemID;
        this.name = name;
        this.quantity = quantity2;
        this.price = price2;
    }

    public String toString() {
        return "{" +
                "itemID='" + itemID + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
