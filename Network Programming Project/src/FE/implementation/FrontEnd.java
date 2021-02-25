package implementation;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.rmi.AlreadyBoundException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.omg.CORBA.*;

import com.sun.javafx.scene.paint.GradientUtils.Parser;

import common.Tuple;
import java.util.ArrayList;
import java.util.Scanner;



public class FrontEnd extends shared.FrontEndInterfacePOA {

	private org.omg.CORBA.ORB orb;
	
	private InetAddress localAddress = InetAddress.getLocalHost();
	private int localPort = 5100;
	private InetAddress sequencerAddress = InetAddress.getLocalHost();
	private int sequencerPort = 5123;
	
	private InetAddress RM1Address = InetAddress.getLocalHost();
	private int RM1Port = 4123;
	private InetAddress RM2Address = InetAddress.getLocalHost();
	private int RM2Port = 4124;
	private InetAddress RM3Address = InetAddress.getLocalHost();
	private int RM3Port = 4125;
	
	private Logger log;
	private String uuid;
	private long stopwatch = System.currentTimeMillis();
	
	private static int resendDelay = 1000;
	
	private DatagramSocket socket;
	
	private ArrayList<Tuple<InetAddress, Integer, String>> RMAddresses = new ArrayList<Tuple<InetAddress, Integer, String>>();
	
	public FrontEnd(ORB orb, String uuid) throws AlreadyBoundException, IOException {
			super();
		
			this.orb = orb;
			this.uuid = uuid;
			
			ReadAddresses();
			System.out.println(this.localAddress + " " + this.localPort);
			this.socket = new DatagramSocket(this.localPort, this.localAddress);
			
			log = initiateLogger(uuid);
			log.info("Front-end starting-up.");
			
			log.info("Front-End started up with port: " + localPort);
			
			
			RMAddresses.add(new Tuple<InetAddress, Integer, String>(RM1Address, RM1Port, null));
			RMAddresses.add(new Tuple<InetAddress, Integer, String>(RM2Address, RM2Port, null));
			RMAddresses.add(new Tuple<InetAddress, Integer, String>(RM3Address, RM3Port, null));
	}
	
	public void shutdown() {
		this.orb.shutdown(false);
	}
	
	
	public String ReturnItem (String customerId, String itemId, String dateOfReturn) {	
		String msgArguments = "RETURN" + "," + customerId + "," + itemId + "," + dateOfReturn;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
		
	}	
	
	
	public String exchangeItem(String customerId, String newItemId, String oldItemId, String dateOfReturn) {
		String msgArguments = "EXCHANGE" + "," + customerId + "," + newItemId + "," + oldItemId + "," + dateOfReturn;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
		
	}
	
	public String PurchaseItem(String customerId, String itemId, String dateOfPurchase) {
		
		String msgArguments = "PURCHASE" + "," + customerId + "," + itemId + "," + dateOfPurchase;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
		
	}
	
	public String[] FindItem(String customerId, String itemDescription) {
		String msgArguments = "FIND" + "," + customerId + "," + itemDescription;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			String[] replyArray = reply.replace("[", "").replace("]", "").split(", ");
			return replyArray;
		} catch (IOException e) {
			return new String[1];
		}
		
	}
	
	public String AddCustomerToWaitList(String itemId, String customerId) {
		String msgArguments = "ADDTOWAITLIST" + "," + itemId + "," + customerId;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
		
	}
	
   public String AddItem(String managerId, String itemId, String itemName, int quantity, double price) {
		String msgArguments = "ADD" + "," + managerId + "," + itemId + "," + itemName + "," + quantity + "," + price;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
   }

	public String RemoveItem(String managerId, String itemId, int quantity) {
		String msgArguments = "REMOVE" + "," + managerId + "," + itemId + "," + quantity;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			
			return reply;
		} catch (IOException e) {
			return e.getMessage();
		}
	}
	
	public String[] ListItemAvailability(String managerId){
		
		String msgArguments = "LIST" + "," + managerId;
		String msg = buildMessage(localAddress, localPort, msgArguments);
		
		sendAnswerMessage(msg, sequencerAddress, sequencerPort);
		
		try {
			String reply = receiveReply(socket);
			String[] replyArray = reply.replace("[", "").replace("]", "").split(", ");
			return replyArray;
		} catch (IOException e) {
			return new String[1];
		}
	}
	
	/**
     * Sends a received message to a process.
     *
     * @param requestAddress the InetAdress of the machine
     * @param requestPort the port to connect to
     */
	public void sendReceivedMessage(InetAddress requestAddress, int requestPort) {
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            byte[] resultBytes = String.format("RECEIVED").getBytes();
            DatagramPacket request = new DatagramPacket(resultBytes, resultBytes.length, requestAddress, requestPort);
            sendSocket.send(request);
        } catch (IOException ex) {
        }
    }

    /**
     * Sends a reliable UDP request to another machine. This is reliable since
     * it sends the message, and if no response it returned in
     * 'ReplicaManager.resendDelay' amount of time then the message is resent.
     *
     * @param message the message to be sent through UDP request
     * @param requestAddress the InetAddress of the machine
     * @param requestPort the port to connect to
     */
    
    public void sendAnswerMessage(String message, InetAddress requestAddress, int requestPort) {
        boolean not_received = true;
        byte[] resultBytes = message.getBytes();
        DatagramPacket request = new DatagramPacket(resultBytes, resultBytes.length, requestAddress, requestPort);
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            sendSocket.setSoTimeout(resendDelay);
            while (not_received) {
            	this.stopwatch = System.currentTimeMillis();
                sendSocket.send(request);
                System.out.println(message);
                try {
                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    sendSocket.receive(reply);
                    String answer = new String(reply.getData());
                    answer = answer.trim();
                    if (answer.equalsIgnoreCase("RECEIVED")) {
                        not_received = false;
                    }
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (IOException ex) {
        }
    }
    
    
    /**
     * Waits for UDP requests. When a request is received, it returns the answer
     */
    private String waitForReply(DatagramSocket socket) {
        try {
            byte[] buffer = new byte[1000];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            socket.receive(request);
            String message = new String(request.getData());
            sendReceivedMessage(request.getAddress(), request.getPort());

            return message;
        } catch (SocketException ex) {
            System.out.println("Could not connect to port, canceling server");
            System.exit(1);
        } catch (IOException ex) {
        }
		return null;
    }
    
    private String receiveReply(DatagramSocket socket) throws IOException {
    	int expectedReplies = RMAddresses.size();
    	ArrayList<Tuple<InetAddress, Integer, String>> repliedRM = new ArrayList<Tuple<InetAddress, Integer, String>>(); 
    	boolean continueRecieving = true;
    	String answer;
    	int count = 0;
    	long timeoutTime = 0;
    	
    	try {
    		while(continueRecieving) {
    			byte[] buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                System.out.println("Receiving now");
                socket.receive(request);
                System.out.println("Message recieved");
                long timeNow = System.currentTimeMillis();
                long timeToReply = timeNow  - stopwatch; 
                count++;
                String message = new String(request.getData());
                
                String[] splitMessage = message.split(";");

                String sender = splitMessage[0];
                String[] args = sender.split(",");
                InetAddress senderAddress = InetAddress.getByName(args[0]);
                int senderPort = Integer.parseInt(args[1]);
                
                Tuple<InetAddress, Integer, String> RMAddress = new Tuple<InetAddress, Integer, String>(senderAddress, senderPort, splitMessage[1]);
                
                System.out.println(senderAddress + "||" + senderPort + "||" + splitMessage[1]);
                
                if (!repliedRM.contains(RMAddress)) {
                	repliedRM.add(RMAddress);
                    sendReceivedMessage(request.getAddress(), request.getPort());
                }
                
                if (count > 1 && (timeoutTime == 0 || timeToReply > timeoutTime))
                	socket.setSoTimeout(10000);
                
                if (expectedReplies == repliedRM.size())
                	continueRecieving = false;
                
    		}
    		
    		answer = getMajority(repliedRM);
    		
    		return answer;
    		
        
        } catch (IOException ex) {
        	answer = getMajority(repliedRM);
    		return answer;
        }
    }
    
    private String getMajority(ArrayList<Tuple<InetAddress, Integer, String>> repliedRM) {
    	
    	String result = "";
		int counter = 0;
		
		for(Tuple<InetAddress, Integer, String> r: repliedRM) {
			if(counter == 0) {
				result = r.getRight();
				counter ++;
			}else if(result.equalsIgnoreCase(r.getRight())) {
				counter ++;
			}else {
				counter--;
			}
			
		}
		if(counter == RMAddresses.size()) {
			return result;
		}
		
		for(Tuple<InetAddress, Integer, String> r: repliedRM) {
			if(!result.equalsIgnoreCase(r.getRight())) {
				notifyFail(r.getLeft(), r.getMiddle());
				return result;
			}
			
		}
		
		if (repliedRM.size() < RMAddresses.size()) {
			ArrayList<Boolean> copy = new ArrayList<>(RMAddresses.size());
			copy.add(false);copy.add(false);copy.add(false);
			
			Tuple<InetAddress, Integer, String> missingRM;
			
			for (int i = 0; i < RMAddresses.size(); i++) {
				for(Tuple<InetAddress, Integer, String> ref: repliedRM) {
					if (ref.getLeft().equals(RMAddresses.get(i).getLeft()) && ref.getMiddle().equals(RMAddresses.get(i).getMiddle())) {
						copy.set(i, true);
					}
				}
			}
			
			for (int i = 0; i < 3; i++) {
				System.out.println(copy.get(i) + "  " + RMAddresses.get(i).getLeft());
				if (!copy.get(i)){
					missingRM = RMAddresses.get(i);
					notifyCrash(missingRM.getLeft(), missingRM.getMiddle());
				}
			}
		}
		
		return result;
    }
    
    private void notifyFail(InetAddress address, int port) {
    	
    	for (Tuple<InetAddress, Integer, String> r: RMAddresses) {
    		String msgArguments = "ERROR" + "," + address.getHostAddress() + ", " + port;
    		String msg = buildMessage(this.localAddress, this.localPort, msgArguments);
    		new Thread(() -> {
    			sendAnswerMessage(msg, r.getLeft(), r.getMiddle());
            }).start();
    	}
    }
    
    private void notifyCrash(InetAddress address, int port) {
    	
    	for (Tuple<InetAddress, Integer, String> r: RMAddresses) {
    		String msgArguments = "CRASH" + "," + address.getHostAddress() + ", " + port;
    		String msg = buildMessage(address, port, msgArguments);
    		new Thread(() -> {
    			sendAnswerMessage(msg, r.getLeft(), r.getMiddle());
            }).start();
    	}
    }
    
    
    private String buildMessage(InetAddress address, int port, String arguments) {
		return address.getHostAddress() + "," + Integer.toString(port) + ";" + arguments;
	}
	
	
	private Logger initiateLogger(String uuid) throws IOException {
		Files.createDirectories(Paths.get("Logs/FrontEnd"));
		
		Logger logger = Logger.getLogger("Logs/FrontEnd/" + uuid + ".log");
		FileHandler fileHandler;
		
		try
		{
			fileHandler = new FileHandler("Logs/FrontEnd/" + uuid + ".log");
			
			logger.setUseParentHandlers(false);
			logger.addHandler(fileHandler);
			
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
		}
		
		catch (IOException e)
		{
			System.err.println("IO Exception " + e);
			e.printStackTrace();
		}
		
		System.out.println("FrontEnd can successfully log.");
		
		return logger;
	}
	
	
	private void ReadAddresses() throws UnknownHostException, FileNotFoundException {
		Scanner fScn = new Scanner(new File("addresses.txt"));
	     String data;

	      while( fScn.hasNextLine() ){
	           data = fScn.nextLine();

	           String[] token = data.split(",");
	           this.localAddress = InetAddress.getByName(token[0]);
	           this.localPort = Integer.parseInt(token[1]);
	           
	           this.sequencerAddress = InetAddress.getByName(token[2]);
	           this.sequencerPort = Integer.parseInt(token[3]);
	           
	           this.RM1Address = InetAddress.getByName(token[4]);
	           this.RM1Port = Integer.parseInt(token[5]);
	           
	           this.RM2Address = InetAddress.getByName(token[6]);
	           this.RM2Port = Integer.parseInt(token[7]);
	           
	           this.RM3Address = InetAddress.getByName(token[8]);
	           this.RM3Port = Integer.parseInt(token[9]);
	      }
	      fScn.close();
	}
	
	
	
}





