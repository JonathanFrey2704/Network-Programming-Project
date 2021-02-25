package Replica.Jonathan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class ONStore {
    public static void main(String args[]) {

        int ONPort = 3000;
        StoreServer ONStore = new StoreServer("ON",3000);

        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(ONPort);
            byte[] buffer;
            System.out.println("Server Started............");

            while (true) {

                buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                String request_string = new String(request.getData()).trim();
                System.out.println("Request received from client: " + request_string);
                String result="";

                if(request_string.startsWith("M1")) {
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.addItem(inputs[0],inputs[1],inputs[2],Integer.parseInt(inputs[3]),Integer.parseInt(inputs[4]));
                }
                else if(request_string.startsWith("M2")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.removeItem(inputs[0],inputs[1],Integer.parseInt(inputs[2]));
                }
                else if(request_string.startsWith("M3")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.listItemAvailability(inputs[0]);
                }
                else if(request_string.startsWith("C1")) {
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.purchaseItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("C2")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.findItem(inputs[0],inputs[1]);
                }
                else if(request_string.startsWith("C3")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.returnItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("C4")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = ONStore.listItemAvailability(inputs[0]);
                }
                else if(request_string.startsWith("PFI")){
                    String[] inputs = request_string.substring(3).split(",");
                    result = ONStore.purchaseForeignItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("FFI")){
                    String input = request_string.substring(3);
                    result = ONStore.findForeignItem(input);
                }
                else if(request_string.startsWith("RFI")){
                    String[] inputs = request_string.substring(3).split(",");
                    result = ONStore.returnForeignItem(inputs[0],inputs[1],inputs[2]);
                }

                DatagramPacket reply = new DatagramPacket(result.getBytes(), result.length(), request.getAddress(), request.getPort());
                aSocket.send(reply);
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }

    }
}
