package store;


import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.rmi.AlreadyBoundException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class StoreServer{
	
	private String region;
	
	private static int port;
	
	private DatagramSocket socket ;
	
	private Map<String, Customer> customerList = new HashMap<String, Customer>();
	private Map<String, Manager> managerList = new HashMap<String, Manager>();
	private Map<String, Product> productList = new HashMap<String, Product>();
	
	private Map<String, String> blacklistedCustomers = new HashMap<String, String>();
	
	private Map<String, Queue<String>> waitList = new HashMap<String, Queue<String>>();
	
	
	public StoreServer(String _region, int _port) {
			super();
		
			region = _region;
			
			port= _port;
			InitializeStore(region);
			
			try {
				socket = new DatagramSocket(getPort(region));
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Thread t = new Thread(new Runnable() {
			    private String region;
			    public Runnable init(String _region) {
			        this.region = _region;
			        return this;
			    }
			    @Override
			    public void run() {
			    	InitializeServerListener(this.region);
			    }
			}.init(region));
			
			t.start();
		
		
	}
	
	
	
	public String returnItem (String customerId, String itemId, String dateOfReturn) {
		
			if (dateOfReturn != null)
				dateOfReturn = dateOfReturn.substring(0,2) + "-" + dateOfReturn.substring(2,4) + "-" + dateOfReturn.substring(4,8);
			
 			String replyMessage = verifyReturn(customerId, itemId, dateOfReturn);
			
			if (replyMessage.equalsIgnoreCase("CanReturn")) {
				
				Customer customer = customerList.get(customerId);
				
				Product product = productList.get(itemId);
				
				LinkedList<Tuple<String, Date, Double>> purchasedProducts = customer.getPurchasedProducts();
				
				for (int i = purchasedProducts.size() - 1; i >= 0; i--) {
					
					Tuple<String, Date, Double> node = purchasedProducts.get(i);
					String itemRegion = ExtractRegion(itemId);
					if (node.getLeft().equalsIgnoreCase(itemId) && itemRegion.equalsIgnoreCase(region)) {
						
						setProductQuantity(product, product.getQuantity() + 1);
			        	setCustomerBudget(customer, customer.getBudget() + node.getRight());
			        	 
			        	purchasedProducts.remove(i);
			        	try {
							UpdateWaitList(itemId);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				        return String.format("SUCCESS: Customer returned item %s", itemId);
					}
					
					else if(node.getLeft().equalsIgnoreCase(itemId)){
						
						replyMessage = ReturnItemAtDifferentStore(customerId, itemId);
						if (replyMessage.contains("SUCCESS")) {
							setCustomerBudget(customer, customer.getBudget() + node.getRight());
			        	 	purchasedProducts.remove(i);
			        	 	return replyMessage;
						}
						else
							return replyMessage;
					}
					
					else
						return "Denied";
					
				}
			}
			else
				return replyMessage;
			
			return replyMessage;
		}
		
	
	
	public String exchangeItem(String customerId, String newItemId, String oldItemId, String dateOfReturn) {
		
		if (dateOfReturn != null)
			dateOfReturn = dateOfReturn.substring(0,2) + "-" + dateOfReturn.substring(2,4) + "-" + dateOfReturn.substring(4,8);
		
		Customer customer = customerList.get(customerId);
		
		if (customer == null) {
			String.format("ERROR: Non customer accessing return item");
		}
		
		String returnMessage, purchaseMessage;
		
		returnMessage = verifyReturn(customerId, oldItemId, dateOfReturn);
		if (returnMessage.equalsIgnoreCase("CanReturn")) {
			
			double budget = customer.getBudget();
			double oldItemPrice = getProductPrice(oldItemId);
			double newItemPrice = getProductPrice(newItemId);
			
			if (newItemPrice < 0)
				return String.format("ERROR: New item %s does not exists", newItemId);
			else if (budget + oldItemPrice < newItemPrice)
				return String.format("ERROR: Customer %s does not have enough budget to exhange item %s with item %s", customerId, oldItemId, newItemId);
			else {
				boolean isReserved = ReserveItem(customerId, newItemId);
				if (isReserved) 
				{
					returnMessage = returnItem(customerId, oldItemId, dateOfReturn);
					if (!returnMessage.contains("SUCCESS"))
						return returnMessage;
					
					
					purchaseMessage = purchaseItem(customerId, newItemId, dateOfReturn);
					if (!purchaseMessage.contains("SUCCESS"))
						return purchaseMessage;
					
					return String.format("SUCCESS: Customer %s exchanged item %s with item %s", customerId, oldItemId, newItemId);
					
				}
				else
					return "CannotReserve";
			}
		}
		else
			return returnMessage;
	}
	
	public String purchaseItem(String customerId, String itemId, String dateOfPurchase) {
		
		Date date = null;
		
		if (dateOfPurchase != null)
			dateOfPurchase = dateOfPurchase.substring(0,2) + "-" + dateOfPurchase.substring(2,4) + "-" + dateOfPurchase.substring(4,8);
		else 
			date = new Date(System.currentTimeMillis());
		
		String itemRegion = ExtractRegion(itemId);
		
		Customer customer = customerList.get(customerId);
		if (customer == null) {
			customer = new Customer(customerId, region);
			customerList.put(customerId, customer);
		}
		
		if (itemRegion.equalsIgnoreCase(region))
		{
			Product product = productList.get(itemId);
			
			if (product != null) {
				
				HashMap<String, String> reservations = product.getReservations();
				
				if ( (reservations.size() == 0 || product.getQuantity() <= reservations.size() || reservations.containsKey(customerId)) && product.getQuantity() != 0) {
					
					try {
						if (date == null)
							date = new SimpleDateFormat("dd-MM-yyyy").parse(dateOfPurchase);
						String status = CompleteTransaction(customerId, itemId, date);
						return status;
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				 
				else {
					String s = addCustomerToWaitList(itemId, customerId);
					return s;
				}
					
				
			}
			else 
				return "ProductMissing";
			
		}
		else {
			String status = "Failed";
			try {
				status = PurchaseItemInDifferentStore(customerId, itemId, dateOfPurchase);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return status;
		}
		
		return "Failed";
	}
	
	public String[] findItem(String customerId, String itemDescription) {
		
		try {
			
			Customer customer = customerList.get(customerId);
			
			if (customer == null) {
				customer = new Customer(customerId, region);
				customerList.put(customerId, customer);
			}
			
			else {
				
				 LinkedList<Product> matches = new LinkedList<Product>();
				
				 String[] regions = {"QC", "ON", "BC"};
				 for (int i = 0; i < regions.length; i++) {
					 if (regions[i] == this.region)
						 matches.addAll(SearchItem(itemDescription));
					 else {
						int port = getPort(regions[i]);
						InetAddress host = InetAddress.getLocalHost();
						
						DatagramSocket socket = new DatagramSocket();
						String message = "FindItem," + itemDescription + ",";
						byte[] m = message.getBytes();
						DatagramPacket request = new DatagramPacket(m, m.length , host, port);
						socket.send(request);
						
						byte[] buffer = new byte[1000];
						DatagramPacket r = new DatagramPacket(buffer, buffer.length);
						socket.receive(r);
						
						ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(r.getData()));
						LinkedList<Product> foundMatches = (LinkedList<Product>) inputStream.readObject();
						
						matches.addAll(foundMatches);
						socket.close();
					 }
					 	
		    	 }
				 
				 LinkedList<String> matchesString = new LinkedList<String>();
				 for (int i = 0; i < matches.size(); i++) {
					 String entry = String.format("%s %.2f %d", matches.get(i).getId(), matches.get(i).getPrice(), matches.get(i).getQuantity());
					 matchesString.add(entry);
				 }
				 
				 String[] array = matchesString.toArray(new String[matchesString.size()]);
				 
				return array;
			}
		}
		catch(Exception ex) {
			//throw ex;
		}
		return null;
	}
	
	public String addCustomerToWaitList(String itemId, String customerId) {
		
		String itemRegion = ExtractRegion(itemId);
		
		if (itemRegion.equalsIgnoreCase(region)) {
			
			Product product = productList.get(itemId);
			
			if (product != null) {
				Queue<String> queue = waitList.get(itemId);
				
				if (queue != null) {
					queue.add(customerId);
				}
				else {
					queue = new LinkedList<String>();
					queue.add(customerId);
					waitList.put(itemId, queue);
				}
				
				return String.format("SUCCESS: Putting customer on waitlist");
			}
			else 
				return "ProductMissing";
			
		}
		else {
			String status = "Failed";
			try {
				status = AddCustomerToWaitListOtherStore(itemId, customerId);
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "ERROR: Could not connect to the server";
		}
	}
	
	   
  
	   public String addItem(String managerId, String itemId, String itemName, int quantity, double price) {
			
			Manager manager = managerList.get(managerId);
			
			if (manager == null)
				return "ERROR: Non manager accessing add item";
			
			else
			{
				String itemRegion = ExtractRegion(itemId);
				
				if (itemRegion.equalsIgnoreCase(region))
				{
					Product product = productList.get(itemId);
					
					
					if (product == null)
					{
						product = new Product(itemId, itemName, quantity, price);
						productList.put(itemId, product);
					}
					
					else
					{
						if (!product.getDescription().equalsIgnoreCase(itemName))
							return String.format("ERROR: Added item name %s does not match item with id %s", itemName, itemId);
						
						else if (product.getPrice() != price)
							return String.format("ERROR: Added item price %f does not match item with id %s", price, itemId);
						else {
							int oldQuantity = product.getQuantity();
							
							setProductQuantity(product, quantity + oldQuantity);
							
							if (oldQuantity == 0 && quantity > 0)
								try {
									UpdateWaitList(itemId);
								} catch (UnknownHostException e) {
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
					}
					
					return String.format("SUCCESS: Added %d item %s to store", quantity, itemId);
					
				}
				else {
					return String.format("ERROR: Adding item ID %s to location %s", itemId, this.region);
				}
					
					
			}
		}
	
		public String removeItem(String managerId, String itemId, int quantity) {
			
			Manager manager = managerList.get(managerId);
			
			if (manager == null)
				return String.format("ERROR: Non manager accessing remove item");
			
			else
			{
				String itemRegion = ExtractRegion(itemId);
							
				if (itemRegion.equalsIgnoreCase(region))
				{
					Product product = productList.get(itemId);
					if (product != null)
					{
						if (quantity < 0)
						{
							String returnMessage = String.format("SUCCESS: Item %s deleted from the store", product.getDescription());
							productList.remove(itemId);
							waitList.remove(itemId);
							return returnMessage;
						}
						else
						{
							int newQuantity = product.getQuantity() - quantity > 0 ? product.getQuantity() - quantity : 0;
							int removed = (product.getQuantity() - quantity < 0 ?product.getQuantity() : quantity);
							setProductQuantity(product, newQuantity);
							
						   return String.format("SUCCESS: Removed %d items %s from the store" + (removed == quantity ? "" : " instead of " + quantity), removed, product.getDescription());
						}
							
					}
					else
						return String.format("ERROR: Removed item ID %s does not exist in store", itemId);
					
					
				}
				return String.format("ERROR: Removed item ID %s does not exist in store", itemId);
			}
		}
	
		public String[] listItemAvailability(String managerId){
		
			Manager manager = managerList.get(managerId);
			
			if (manager == null)
				throw new SecurityException();
			
			else
			{
				LinkedList<String> allItems = new LinkedList<String>();
				String logString = "";
				for (Map.Entry<String, Product> map : productList.entrySet()) {
					Product product = map.getValue();
					String entry = String.format("%s %s %.2f %d", product.getId(), product.getDescription(), product.getPrice(), product.getQuantity());
					allItems.add(entry);
					logString = logString + entry + "\n";
				}
				 
				 String[] array = allItems.toArray(new String[allItems.size()]);
				 
				return array;
				
			}
		}
		
	
	
	   
	   private void InitializeServerListener(String region) {
		   try {

			  
			   byte[] buffer = new byte[10000];
			   
			   while(true){
				   DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				   socket.receive(request);
				   
				   String data = new String(request.getData());
				   String[] values = data.split(",");
				   
				   String replyMessage = "";
				   byte r[] = new byte [1000];
				   
				   if (values.length > 0 && values[0].equalsIgnoreCase("PurchaseItem")) {
					   String customerId = values[1];
					   String itemId = values[2];
					   String dateOfPurchase = values[3];
					   double budget =Double.parseDouble(values[4]);
					   
					   
					   Product product = productList.get(itemId);
					   if (product != null) {
						   
						  
						  if (!IsCustomerBlackListed(customerId)){
							  if (budget >= product.getPrice()) {
								  HashMap<String,String> reservations = product.getReservations();
								  if ((reservations.size() == 0 || product.getQuantity() <= reservations.size() || reservations.containsKey(customerId)) && product.getQuantity() != 0) {
										Date date = new SimpleDateFormat("dd-MM-yyyy").parse(dateOfPurchase);
										
										String status = CompleteTransaction(customerId, itemId, date);
										
										blacklistedCustomers.put(customerId, customerId);
										replyMessage =  status + "," + product.getPrice();
									
								  }
								  else {
										replyMessage =  addCustomerToWaitList(itemId, customerId);
								  }
								  
							  } 
							  else  
								  replyMessage =  String.format("ERROR: Customer could not afford item %s", itemId);
								  
							
						  }
						  else
							  replyMessage = String.format("ERROR: Cannot purchase multiple items from inter-province store");
						}
						else {
							replyMessage = String.format("ERROR: Purchased item ID %s does not exist in store", itemId);
						}
					   
					   r = replyMessage.getBytes();
				   }
				   
				   	else if (values.length > 0 && values[0].equalsIgnoreCase("ReturnItem")) {
					   
					   String customerId = values[1];
					   String itemId = values[2];
					  

					   Product product = productList.get(itemId);
					   if (product != null) {
						   product.setQuantity(product.getQuantity() + 1);
						   blacklistedCustomers.remove(customerId);
						   UpdateWaitList(itemId);
						   replyMessage = String.format("SUCCESS: Customer returned item %s", itemId);
					   }
					   else
						   replyMessage = String.format("ERROR: Purchased item ID %s does not exist in store", itemId);
					   
					   r = replyMessage.getBytes();
				   }
				   
				 	else if (values.length > 0 && values[0].equalsIgnoreCase("ReserveItem")) {
						   
						   String customerId = values[1];
						   String itemId = values[2];
						  

						   boolean isReserved = ReserveItem(customerId, itemId);
						   if (isReserved)
							   replyMessage = "Reserved";
						   else
							   replyMessage = "CannoteReserve";
						   
						   r = replyMessage.getBytes();
				   }
					   
				   
				   else if (values.length > 0 && values[0].equalsIgnoreCase("AddToWaitList")) {
					   
					   String customerId = values[1];
					   String itemId = values[2];
					   
					   replyMessage = addCustomerToWaitList( itemId, customerId);
					   
					   r = replyMessage.getBytes();
				   }
				   
				   	else if (values.length > 0 && values[0].equalsIgnoreCase("GetProductPrice")) {
					   
					   String itemId = values[1];
					  
					   double price = getProductPrice(itemId);
					   
					   ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);
					   byteBuffer.putDouble(price);
					   r = byteBuffer.array();
				   }
				   else if (values.length > 0 && values[0].equalsIgnoreCase("PurchaseFromWaitList")) {
					   
					   String customerId = values[1];
					   String itemId = values[2];
					   double price = Double.parseDouble(values[3]);
					  
					   
					   Customer customer = customerList.get(customerId);
					   if (customer != null){
							double newBudget = customer.getBudget() - price;
							
							if (newBudget > 0) 
							{
								setCustomerBudget(customer, newBudget);
								LinkedList<Tuple<String, Date, Double>> customerPurchaseOrder = customer.getPurchasedProducts();
								Tuple<String, Date, Double> newPurchase = new Tuple<String, Date, Double> (itemId, new Date(), price);
								customerPurchaseOrder.add(newPurchase);
								
								replyMessage = String.format("SUCCESS: Purchased item %s from sever %s", itemId, this.region);
							}
							
								
							else
								replyMessage = "MissingFunds";
						}
					   
					   r = replyMessage.getBytes();
				   }
				   
				   else if (values.length > 0 && values[0].equalsIgnoreCase("FindItem"))
				   {
					   String itemDescription = values[1];
					   
					   itemDescription = itemDescription.replace("\"", "");
					   
					    LinkedList<Product> matches = SearchItem(itemDescription);
					   
					    ByteArrayOutputStream out = new ByteArrayOutputStream();
					    ObjectOutputStream outputStream = new ObjectOutputStream(out);
					    outputStream.writeObject(matches);
					    outputStream.close();

					    r = out.toByteArray();
				   }
				   System.out.println("looped");
				   DatagramPacket reply = new DatagramPacket(r, r.length , request.getAddress(), request.getPort());
				   socket.send(reply);
				   
				   values = null;
			   }
		   }
		   catch(Exception ex){
			   System.out.println(ex);
		   }
	   }
	   
	
	private int getPort(String region) {
		switch (region) {
			case "QC":
				return 4005;
			case "ON":
				return 4006;
			case "BC":
				return 4007;
			default:
				return 0;
			}
			
	}
	
	
	private void InitializeStore(String region) {
		Manager manager1 = new Manager(region+ "M" + 1000, region);
		Manager manager2 = new Manager(region+ "M" + 1001, region);
		Manager manager3 = new Manager(region+ "M" + 1002, region);
		
		managerList.put(manager1.getUserId(), manager1);
		managerList.put(manager2.getUserId(), manager2);
		managerList.put(manager3.getUserId(), manager3);
		
		Customer customer1 = new Customer(region+ "U" + 1000, region);
		Customer customer2 = new Customer(region+ "U" + 1001, region);
		Customer customer3 = new Customer(region+ "U" + 1002, region);
		
		customerList.put(customer1.getUserId(), customer1);
		customerList.put(customer2.getUserId(), customer2);
		customerList.put(customer3.getUserId(), customer3);
		
		
		
	}
	
	private String AddCustomerToWaitListOtherStore(String itemId, String customerId) throws IOException {
			
			String itemRegion = ExtractRegion(itemId);
			
			int port = getPort(itemRegion);
			InetAddress host = InetAddress.getLocalHost();
			
			DatagramSocket socket = new DatagramSocket();
			String message = "AddToWaitList," + customerId + "," + itemId + ",";
			byte[] m = message.getBytes();
			DatagramPacket request = new DatagramPacket(m, m.length , host, port);
			socket.send(request);
			
			byte[] buffer = new byte[1000];
			DatagramPacket r = new DatagramPacket(buffer, buffer.length);
			socket.receive(r);
			
			String status = new String(r.getData()).trim();
			socket.close();
			return status;
		}
	
	private void UpdateWaitList(String itemId) throws IOException {
		 
		 Queue<String> queue = waitList.get(itemId);
		 if (queue != null)
		 {
			 while (!queue.isEmpty()) {
				 String customerId = queue.poll();
				 if (customerId != null)
				 {
					 String customerRegion = ExtractRegion(customerId);
					 if (customerRegion.equalsIgnoreCase(region))
					 {
						 purchaseItem(customerId, itemId, null);
					 }
					 else
					 {
						 Product product = productList.get(itemId);
						 if (product != null){
							String itemRegion = ExtractRegion(customerId);
							int port = getPort(itemRegion);
							InetAddress host = InetAddress.getLocalHost();
							
							DatagramSocket socket = new DatagramSocket();
							String message = "PurchaseFromWaitList," + customerId + "," + itemId + "," + product.getPrice() +",";
							byte[] m = message.getBytes();
							DatagramPacket request = new DatagramPacket(m, m.length , host, port);
							socket.send(request);
							
							byte[] buffer = new byte[10000];
							DatagramPacket r = new DatagramPacket(buffer, buffer.length);
							socket.receive(r);
							
							String replyMessage = new String(r.getData());
							replyMessage = replyMessage.trim();
							socket.close();
							if (replyMessage.contains("SUCCESS"))
							{
								setProductQuantity(product, product.getQuantity() - 1);
							}
							
						 }
						
					 }
				 }
			 }
		 }
		 
	}
	
	
	
	private String CompleteTransaction(String customerId, String itemId, Date dateOfPurchase) {
		Product product = productList.get(itemId);
		Customer customer = customerList.get(customerId);
		
		int newQuantity = product.getQuantity() - 1;
		
		if (customer != null){
			double newBudget = customer.getBudget() - product.getPrice();
			
			if (newBudget >= 0) 
			{
				setCustomerBudget(customer, newBudget);
				LinkedList<Tuple<String, Date, Double>> customerPurchaseOrder = customer.getPurchasedProducts();
				Tuple<String, Date, Double> newPurchase = new Tuple<String, Date, Double> (itemId, dateOfPurchase, product.getPrice());
				customerPurchaseOrder.add(newPurchase);
			}
			
				
			else
				return String.format("ERROR: Customer could not afford item %s", itemId);
		}
		
		
		setProductQuantity(product, newQuantity);
		
		if (product.getReservations().containsKey(customerId))
			product.getReservations().remove(customerId);
		return String.format("SUCCESS: Purchased item %s from sever %s", itemId, this.region);
		
	}
	
	
	private String PurchaseItemInDifferentStore(String customerId, String itemId, String dateOfPurchase) throws UnknownHostException
	{
		try {
			
			Customer customer = customerList.get(customerId);
			
			if (customer != null) {
				Date date = new SimpleDateFormat("dd-MM-yyyy").parse(dateOfPurchase);
				
				String itemRegion = ExtractRegion(itemId);
				int port = getPort(itemRegion);
				InetAddress host = InetAddress.getLocalHost();
				
				DatagramSocket socket = new DatagramSocket();
				String message = "PurchaseItem," + customerId + "," + itemId + "," + dateOfPurchase.toString() + "," + customer.getBudget() +",";
				byte[] m = message.getBytes();
				DatagramPacket request = new DatagramPacket(m, m.length , host, port);
				socket.send(request);
				
				byte[] buffer = new byte[1000];
				DatagramPacket r = new DatagramPacket(buffer, buffer.length);
				socket.receive(r);
				
				String replyMessage = new String(r.getData());
				
				String[] reply = replyMessage.split(",");
				
				socket.close();
				if (reply[0].contains("SUCCESS")){
					
					double price = Double.parseDouble(reply[1]);
					setCustomerBudget(customer,customer.getBudget()- price);
					LinkedList<Tuple<String, Date, Double>> customerPurchaseOrder = customer.getPurchasedProducts();
					Tuple<String, Date, Double> newPurchase = new Tuple<String, Date, Double> (itemId, date, price);
					customerPurchaseOrder.add(newPurchase);
					
				}
				
				return reply[0];
			}
			else
				return "CustomerMissing";
			
			
		}
		catch(SocketException e) {
		}
		catch(IOException e) {
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
	private synchronized String ReturnItemAtDifferentStore(String customerId, String itemId) {
		
		try {
			String itemRegion = ExtractRegion(itemId);
			int port = getPort(itemRegion);
			InetAddress host;
			host = InetAddress.getLocalHost();
			
			DatagramSocket socket = new DatagramSocket();
			String message = "ReturnItem," + customerId + "," + itemId + ",";
			byte[] m = message.getBytes();
			DatagramPacket request = new DatagramPacket(m, m.length , host, port);
			socket.send(request);
			
			byte[] buffer = new byte[1000];
			DatagramPacket r = new DatagramPacket(buffer, buffer.length);
			socket.receive(r);
			
			String status = new String(r.getData()).trim();
			
			socket.close();
			return status;
		}
		catch (Exception ex) {
			return "Failed";
		}
			
		
		
	}
	
	private String verifyReturn(String customerId, String itemId, String dateOfReturn) {
		
		Customer customer = customerList.get(customerId);
		
		if (customer == null)
			throw new SecurityException();
		
		LinkedList<Tuple<String, Date, Double>> purchasedProducts = customer.getPurchasedProducts();
		
		for (int i = purchasedProducts.size() - 1; i >= 0; i--) {
			
			Tuple<String, Date, Double> node = purchasedProducts.get(i);
			
			if (node.getLeft().equalsIgnoreCase(itemId)) {
				Date now = new Date();
				
		        Calendar c = Calendar.getInstance();
		        c.setTime(node.getMiddle());

		        c.add(Calendar.DATE, -30);

		        // convert calendar to date
		        Date minDate = c.getTime();
		        
		        if (minDate.before(now) || !minDate.after(now)) 
		        	return "CanReturn";
		        else
		        	return String.format("ERROR: Returned item %s has passed return policy deadline", itemId);
				        	
			}
		}
		
		return String.format("ERROR: Returned item ID %s does not exist with customer", itemId);
	}
	
	private synchronized void setProductQuantity(Product p, int quantity) {
		p.setQuantity(quantity);
	}
	
	private synchronized void setCustomerBudget(Customer c, double budget) {
		c.setBudget(budget);
	}
	
	private LinkedList<Product> SearchItem(String itemDescription) {
		
		LinkedList<Product> matches = new LinkedList<Product>();
		
		for (Map.Entry<String, Product> map : productList.entrySet()) {
			Product product = map.getValue();
			String description = product.getDescription();
			if (description.equalsIgnoreCase(itemDescription))
				matches.add(product);
		}
		
		return matches;
	}
	
	private double getProductPrice(String itemId) {
		String itemRegion = ExtractRegion(itemId);
		
		if (itemRegion.equalsIgnoreCase(this.region)) {
			Product p = productList.get(itemId);
			if (p != null)
				return p.getPrice();
			else
				return -1;
					
		}
		else {
			try {
			int port = getPort(itemRegion);
			InetAddress host;
			host = InetAddress.getLocalHost();
			
			DatagramSocket socket = new DatagramSocket();
			String message = "GetProductPrice," + itemId + ",";
			byte[] m = message.getBytes();
			DatagramPacket request = new DatagramPacket(m, m.length , host, port);
			socket.send(request);
			
			byte[] buffer = new byte[1000];
			DatagramPacket r = new DatagramPacket(buffer, buffer.length);
			socket.receive(r);
			
			Double price = ByteBuffer.wrap(r.getData()).getDouble();
			
			socket.close();
			return price;
			}
			catch (Exception ex) {
				return -1;
			}
		}
			
	}
	
	private synchronized boolean ReserveItem(String customerId, String itemId) {
		String itemRegion = ExtractRegion(itemId);
		
		if (itemRegion.equalsIgnoreCase(this.region)) {
			Product p = productList.get(itemId);
			
			
			HashMap<String,String> reservations = p.getReservations();
			
			if (p.getQuantity() > 0 && reservations.get(customerId) == null) {
				reservations.put(customerId, customerId);
				return true;
			}
			else
				return false;
				
		}
		else {
			try {
			int port = getPort(itemRegion);
			InetAddress host;
			host = InetAddress.getLocalHost();
			
			DatagramSocket socket = new DatagramSocket();
			String message = "ReserveItem," + customerId + "," + itemId + ",";
			byte[] m = message.getBytes();
			DatagramPacket request = new DatagramPacket(m, m.length , host, port);
			socket.send(request);
			
			byte[] buffer = new byte[1000];
			DatagramPacket r = new DatagramPacket(buffer, buffer.length);
			socket.receive(r);
			
			String status = new String(r.getData());
			
			socket.close();
			if (status.contains("Reserved"))
				return true;
			else
				return false;
			}
			catch (Exception ex) {
				return false;
			}
		}
	
		
		
			
	}
	
	public void close() {
		socket.close();
	}


	private boolean IsCustomerBlackListed(String customerId) {
		String customer = blacklistedCustomers.get(customerId);
		return customer != null;
	}
	
	private String ExtractRegion(String id) {
		return id.substring(0,2);
	}

}
