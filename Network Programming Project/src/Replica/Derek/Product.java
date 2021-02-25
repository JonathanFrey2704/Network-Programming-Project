package store;

import java.io.Serializable;
import java.util.HashMap;

public class Product implements Serializable {

	private String id;
	private String description;
	private double price;
	private int quantity;
	private HashMap<String, String> reservations;
	
	public Product(String _id, String _description, int _quantity, double _price){
		setDescription(_description);
		setId(_id);
		setPrice(_price);
		setQuantity(_quantity);
		reservations = new HashMap<String, String>();
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return this.toString();
	}

	public HashMap<String, String> getReservations() {
		return reservations;
	}

	public void setReservations(HashMap<String, String> reservations) {
		this.reservations = reservations;
	}
	
}

