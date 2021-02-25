package sequencer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Sequencer {

    DatagramSocket validitySocket;
    DatagramSocket sequencerSocket;

    InetAddress localAddress;
    InetAddress sequencerAddress;
    InetAddress validityAddress;
    InetAddress RM1Address;
    InetAddress RM2Address;
    InetAddress RM3Address;

    int localPort;
    int sequencerPort;
    int validityPort;
    int RM1Port;
    int RM2Port;
    int RM3Port;

    int sequence_number;

    
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
            
            this.validityAddress = InetAddress.getByName(token[2]);
            this.validityPort = 5124;

            this.RM1Address = InetAddress.getByName(token[4]);
            this.RM1Port = Integer.parseInt(token[5]);

            this.RM2Address = InetAddress.getByName(token[6]);
            this.RM2Port = Integer.parseInt(token[7]);

            this.RM3Address = InetAddress.getByName(token[8]);
            this.RM3Port = Integer.parseInt(token[9]);

        }
        fScn.close();
    }


    public Sequencer() throws UnknownHostException, SocketException, FileNotFoundException {

        ReadAddresses();
        sequence_number = 0;
        this.sequencerSocket = new DatagramSocket(this.sequencerPort, this.sequencerAddress);
        this.validitySocket = new DatagramSocket(this.validityPort, this.validityAddress);
    }

    /**
     * Sends a received message to a process.
     *
     * @param requestAddress the InetAdress of the machine
     * @param requestPort the port to connect to
     */
	public static void sendReceivedMessage(InetAddress requestAddress, int requestPort) {
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            byte[] resultBytes = String.format("RECEIVED").getBytes();
            DatagramPacket request = new DatagramPacket(resultBytes, resultBytes.length, requestAddress, requestPort);
            sendSocket.send(request);
        } catch (IOException ex) {
        }
    }
	
    public void run() throws IOException {

        boolean not_received = true;
        byte[] buffer;

        while (true) {

            buffer = new byte[1000];
            DatagramPacket FE_request = new DatagramPacket(buffer, buffer.length);
            sequencerSocket.receive(FE_request);
            sendReceivedMessage(FE_request.getAddress(), FE_request.getPort());
           
            String request = new String(FE_request.getData());
            System.out.println(request);
            String[] splitMessage = request.split(";");
            String header = splitMessage[0] +","+ sequence_number ;
            sequence_number++;

            String response = header + ";" + splitMessage[1];
            not_received =true;

            while (not_received) {

                DatagramPacket request1 = new DatagramPacket(response.getBytes(), response.length(), RM1Address, RM1Port);
                DatagramPacket request2 = new DatagramPacket(response.getBytes(), response.length(), RM2Address, RM2Port);
                DatagramPacket request3 = new DatagramPacket(response.getBytes(), response.length(), RM3Address, RM3Port);
                
                System.out.println(RM1Address);
                System.out.println(RM2Address);
                System.out.println(RM3Address);
                
                validitySocket.send(request1);
                validitySocket.send(request2);
                validitySocket.send(request3);
                validitySocket.setSoTimeout(1000);
                try{
                    byte[] buff1 = new byte[1000];
                    byte[] buff2 = new byte[1000];
                    byte[] buff3 = new byte[1000];

                    DatagramPacket reply1 = new DatagramPacket(buff1, buff1.length);
                    DatagramPacket reply2 = new DatagramPacket(buff2, buff2.length);
                    DatagramPacket reply3 = new DatagramPacket(buff3, buff3.length);

                    validitySocket.receive(reply1);
                    validitySocket.receive(reply2);
                    validitySocket.receive(reply3);
                    
                 
                    
                    if (new String(reply1.getData()).trim().equalsIgnoreCase("RECEIVED") && new String(reply2.getData()).trim().equalsIgnoreCase("RECEIVED") && new String(reply3.getData()).trim().equalsIgnoreCase("RECEIVED")) {
                        not_received = false;
                    }
                } catch (SocketTimeoutException e) {
                }
            }

        }
    }
    public static void main(String args[]){
    	
    	
        Sequencer seq = null;
		try {
			seq = new Sequencer();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			seq.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
	   
    }



}
