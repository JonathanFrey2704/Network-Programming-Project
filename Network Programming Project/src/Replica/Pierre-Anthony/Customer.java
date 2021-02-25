package store;


import java.util.Date;
import java.util.LinkedList;

public class Customer extends User {
	
	private double budget = 1000.00;
	private LinkedList<Tuple<String, Date, Double>> purchasedProducts;
	
	public Customer(String _userId, String _region) {
		super(_userId, _region);
		setPurchasedProducts(new LinkedList<Tuple<String, Date, Double>>());
	}
	

	public double getBudget() {
		return budget;
	}

	public void setBudget(double budget) {
		this.budget = budget;
	}

	public LinkedList<Tuple<String, Date, Double>> getPurchasedProducts() {
		return purchasedProducts;
	}

	public void setPurchasedProducts(LinkedList<Tuple<String, Date, Double>> purchasedProducts) {
		this.purchasedProducts = purchasedProducts;
	}

	
}
