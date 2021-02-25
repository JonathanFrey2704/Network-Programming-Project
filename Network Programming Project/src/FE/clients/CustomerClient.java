package clients;



import common.Helper;
import shared.FrontEndInterface;

import org.omg.CORBA.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CustomerClient {
	
	private static CustomerClient client;
	private static String userId;
	private static String uuId;
	private static ORB orb;	
	private static Logger log;
	
	public CustomerClient(ORB orb, String uuId) {
        
		 this.orb = orb;
	     this.uuId = uuId;
        
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
	public void PurchaseItem(String customerId, String itemId, String dateOfPurchase) {
		try {
			
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "PurchaseItem(" +customerId +", " + itemId + ", " + dateOfPurchase + ")";
				log.info(logMethod);
				
				String status = server.PurchaseItem(customerId, itemId, dateOfPurchase);
				status = status.trim();
				if (status.equalsIgnoreCase("Waitlist"))
				{
					while (true)
					{
						log.info(logMethod + " has replied with " + status);
						
						System.out.println("Quantity for this item is 0. Would you like to be added to the waitlist? y/n");
						Scanner scanner = new Scanner(System.in);
						
						String choice = scanner.nextLine();
						if (choice.equalsIgnoreCase("y")) {
							logMethod = "AddCustomerToWaitList(" + itemId +", " + customerId + ")";
							log.info(logMethod);
							status = server.AddCustomerToWaitList(itemId, customerId);
							System.out.println(status);
							log.info(logMethod + " has replied with " + status);
							break;
						}
						else if (choice.equalsIgnoreCase("n"))
							break;
						else
							break;
					}
					
				}
				else {
					System.out.println(status);
					log.info(logMethod + " has replied with " + status);
				}
					
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in customer client: " + e);
	    } 
	}
	public void FindItem(String customerId, String itemDescription) throws Exception{
		
		try {
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "FindItem(" +customerId +", " + itemDescription + ")";
				log.info(logMethod);
				
				
				String[] items = server.FindItem(customerId, itemDescription);
				
				String reply = "";
				if (items.length > 0)
					for (int i = 0; i < items.length; i++) {
						reply = reply + items[i] + "\n";
						
					}
				else
					reply = "No items";
				
				System.out.println(reply);
				log.info(logMethod + " has replied with " + reply);
				
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in customer client: " + e);
	    } 
	}
	

	
	public void ReturnItem (String customerId, String itemId, String dateOfReturn) {
		try {
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "ReturnItem(" +customerId +", " + itemId + ", " + dateOfReturn + ")";
				log.info(logMethod);
				
				String status = server.ReturnItem(customerId, itemId, dateOfReturn);
				
				System.out.println(status);
				log.info(logMethod + " has replied with " + status);
				
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in customer client: " + e);
	    } 
	}
	
	public void ExchangeItem (String customerId, String newItemId, String oldItemId, String dateOfReturn) {
		try {
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "ExchangeItem(" +customerId +", " + newItemId + ", "  + oldItemId + ", "+ dateOfReturn + ")";
				log.info(logMethod);
				
				String status = server.exchangeItem(customerId, newItemId, oldItemId, dateOfReturn);
				
				System.out.println(status);
				log.info(logMethod + " has replied with " + status);
				
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in customer client: " + e);
	    } 
	}
	
	
	private static boolean ValidateId(String id) {
		Pattern p = Pattern.compile("(QC|ON|BC)(U)([0-9]){4}");
   	 	Matcher m = p.matcher(id);
   	 	boolean b = m.matches();
   	 	
   	 	return b;
	}
	
	private static boolean ValidateItemId(String id) {
		Pattern p = Pattern.compile("(QC|ON|BC)([0-9]){4}");
   	 	Matcher m = p.matcher(id);
   	 	boolean b = m.matches();
   	 	
   	 	return b;
	}
	
	
	private static void EnterSystem() throws Exception {
		boolean loop = true;
		Scanner scanner = new Scanner(System.in);
		System.out.println("Valid Id. Welcome.");
		System.out.println("Here are your options");
		
		
		while (loop)
		{
			PrintInstructions();
			int choice = scanner.nextInt();
			
			switch (choice) {
			case 1:
				InputPurchaseItem();
				break;
			case 2:
				InputReturnItem();
				break;
			case 3:
				InputFindItem();
				break;
			case 4:
				InputExchangeItem();
				break;
			
			case 5: 
				loop = false;
				break;
				
			case 6:
				System.exit(0);
			
			default:
				System.out.println("Incorrect value");
			}
			
		}
	}
	
	private static void InputPurchaseItem()  {
		Scanner scanner = new Scanner(System.in);
		
		String itemId;
		String date;
		
		while (true)
		{
			System.out.println("Enter itemId");
			itemId = scanner.nextLine();
			if (ValidateItemId(itemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		while (true)
		{
			System.out.println("Enter date (ddMMyyyy)");
			date = scanner.nextLine();
			
			try {
				new SimpleDateFormat("ddMMyyyy").parse(date);
				break;
			}
			catch (ParseException e){
				System.out.println("Date is not in correct format. Try again");
			}
		}
		
		
		try {
			client.PurchaseItem(userId, itemId, date);
		}  catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void InputReturnItem() {
		Scanner scanner = new Scanner(System.in);
		
		String itemId;
		String date;
		
		while (true)
		{
			System.out.println("Enter itemId");
			itemId = scanner.nextLine();
			if (ValidateItemId(itemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		while (true)
		{
			System.out.println("Enter date (ddMMyyyy)");
			date = scanner.nextLine();
			
			try {
				new SimpleDateFormat("ddMMyyyy").parse(date);
				break;
			}
			catch (ParseException e){
				System.out.println("Date is not in correct format. Try again");
			}
		}
		
		
		try {
			client.ReturnItem(userId, itemId, date);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void InputExchangeItem() {
		Scanner scanner = new Scanner(System.in);
		
		String oldItemId;
		String newItemId;
		String date;
		
		while (true)
		{
			System.out.println("Enter old itemId");
			oldItemId = scanner.nextLine();
			if (ValidateItemId(oldItemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		while (true)
		{
			System.out.println("Enter new itemId");
			newItemId = scanner.nextLine();
			if (ValidateItemId(newItemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		while (true)
		{
			System.out.println("Enter date (ddMMyyyy)");
			date = scanner.nextLine();
			
			try {
				new SimpleDateFormat("ddMMyyyy").parse(date);
				break;
			}
			catch (ParseException e){
				System.out.println("Date is not in correct format. Try again");
			}
		}
		
		
		try {
			client.ExchangeItem(userId, newItemId, oldItemId, date);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void InputFindItem() throws Exception {
		Scanner scanner = new Scanner(System.in);
		
		String itemName;
	
		
		System.out.println("Enter item description");
		itemName = scanner.nextLine();
		
		
		try {
			client.FindItem(userId, itemName);
		}  catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	private static void PrintInstructions() {
		System.out.println("Please enter the number of the option you wish to choose: \n" +
				"1: Purchase an item \n" +
				"2: Refund an item \n" +
				"3: Find an item \n" +
				"4: Exchange an item \n" +
				"5: Change userID \n" +
				"6: Exit system");
	}
	
    public static void main(String args[]) {
    
    	  try {
        	  
        	  UUID uuid = UUID.randomUUID();
        	  
        	  Helper.initializeORB(args, uuid.toString());
        	  
        	  ORB orb = ORB.init(args, null);
    		    
              client = new CustomerClient(orb, uuid.toString());
              while (true)
              { 
             	 Scanner scanner = new Scanner(System.in);
            	 System.out.println("Welcome. Please enter your id");
            	
            	 String id = scanner.nextLine();
            	 
            	 if (ValidateId(id)) {
            		 client.setUserId(id);
            		 log = Helper.initiateLogger(userId);
            		 log.info(userId+ " has logged in");
            		 EnterSystem();
            	 }
            	 else
            		 System.out.println("Oops, bad id");
              }
             
             
          } 
          catch (Exception ex) {
             ex.printStackTrace( );
          } 
    }
   
}