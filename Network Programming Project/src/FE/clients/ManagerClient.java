package clients;


import common.Helper;
import shared.FrontEndInterface;

import org.omg.CORBA.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* This class represents the object client for a distributed
* object of class SomeImpl, which implements the remote 
* interface SomeInterface.
*/

public class ManagerClient {
	
	private static ManagerClient client;
	private static String userId;
	private static String uuId;
	private static ORB orb;	
	private static Logger log;

	
	public ManagerClient(ORB orb, String uuId) {
        
        this.orb = orb;
        this.uuId = uuId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public void AddItem(String managerId, String itemId, String itemName, int quantity, double price) {
		
		try {
		
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "AddItem(" + managerId +", " + itemId + ", " + itemName + ", " + quantity + ", " + price + ")";
				
				log.info(logMethod);
				
				String status = server.AddItem(managerId, itemId, itemName, quantity, price);
				
				System.out.println(status);
				log.info(logMethod + " has replied with " + status);
				
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in Client: " + e);
	    } 
		
	}
	
	
	public void RemoveItem(String managerId, String itemId, int quantity) throws  SecurityException {
		try {
			
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "RemoveItem(" + managerId +", " + itemId + "," + quantity + ")";
				log.info(logMethod);
				
				String status = server.RemoveItem(managerId, itemId, quantity);
				
				System.out.println(status);
				log.info(logMethod + " has replied with " + status);
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in Client: " + e);
	    } 
	}
	
	public void ListItemAvailability(String managerId) throws SecurityException  {
		try {
			
			FrontEndInterface server = Helper.getInterface(orb, uuId);
			
			if (server != null)
			{
				String logMethod = "RemoveItem(" + managerId + ")";
				log.info(logMethod);
				
				String[] matches = server.ListItemAvailability(managerId);
				
				String reply = "";
				if (matches.length > 0)
					for (int i = 0; i < matches.length; i++) {
						reply = reply + matches[i] + "\n";
						
					}
				else
					reply = "No items";
				System.out.println(reply);
				log.info(logMethod + " has replied with " + reply);
				
			}
		}
		catch (Exception e) {
	         System.out.println("Exception in Client: " + e);
	    } 
	}
	
	private static boolean ValidateId(String id) {
		Pattern p = Pattern.compile("(QC|ON|BC)(M)([0-9]){4}");
   	 	Matcher m = p.matcher(id);
   	 	boolean b = m.matches();
   	 	
   	 	return b;
	}
	
	private static void EnterSystem() throws SecurityException {
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
				InputAddItem();
				break;
			case 2:
				InputRemoveItem();
				break;
			case 3:
				client.ListItemAvailability(userId);
				break;
			
			case 4: 
				loop = false;
				break;
				
			case 5:
				System.exit(0);
			
			default:
				System.out.println("Incorrect value");
			}
			
		}
	}
	
	private static void InputAddItem() {
		Scanner scanner = new Scanner(System.in);
		
		String itemId;
		String itemName;
		int quantity;
		double price;
		
		while (true)
		{
			System.out.println("Enter itemId");
			itemId = scanner.nextLine();
			if (Helper.ValidateItemId(itemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		System.out.println("Enter itemName");
		itemName = scanner.nextLine();
		
		System.out.println("Enter itemQuantity");
		quantity = scanner.nextInt();
		
		System.out.println("Enter itemPrice");
		price = scanner.nextDouble();
		
		try {
			client.AddItem(userId, itemId, itemName, quantity, price);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	private static void InputRemoveItem() {
		Scanner scanner = new Scanner(System.in);
		
		String itemId;
		String itemName;
		int quantity;
		double price;
		
		while (true)
		{
			System.out.println("Enter itemId");
			itemId = scanner.nextLine();
			if (Helper.ValidateItemId(itemId))
				break;
			else
				System.out.println("Invalid itemId");
		}
		
		System.out.println("Enter itemQuantity");
		quantity = scanner.nextInt();
		
		
		try {
			client.RemoveItem(userId, itemId, quantity);
		}  catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	private static void PrintInstructions() {
		System.out.println("Please enter the number of the option you wish to choose: \n" +
				"1: Add a new item or update inventory \n" +
				"2: Remove an item or remove inventory \n" +
				"3: Check available items \n" +
				"4: Change userID \n" +
				"5: Exit system");
	}
	
    public static void main(String args[]) {
      try {
    	  
    	  UUID uuid = UUID.randomUUID();
    	  
    	  Helper.initializeORB(args, uuid.toString());
    	  
    	  ORB orb = ORB.init(args, null);
		    
          client = new ManagerClient(orb, uuid.toString());
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