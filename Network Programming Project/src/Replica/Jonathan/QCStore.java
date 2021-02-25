package Replica.Jonathan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;


public class QCStore {
    public static void main(String args[]) {

        int QCPort = 2000;
        StoreServer QCStore = new StoreServer("QC",2000);

        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(QCPort);
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
                    result = QCStore.addItem(inputs[0],inputs[1],inputs[2],Integer.parseInt(inputs[3]),Integer.parseInt(inputs[4]));
                }
                else if(request_string.startsWith("M2")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.removeItem(inputs[0],inputs[1],Integer.parseInt(inputs[2]));
                }
                else if(request_string.startsWith("M3")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.listItemAvailability(inputs[0]);
                }
                else if(request_string.startsWith("C1")) {
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.purchaseItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("C2")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.findItem(inputs[0],inputs[1]);
                }
                else if(request_string.startsWith("C3")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.returnItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("C4")){
                    String[] inputs = request_string.substring(2).split(",");
                    result = QCStore.exchangeItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("PFI")){
                    String[] inputs = request_string.substring(3).split(",");
                    result = QCStore.purchaseForeignItem(inputs[0],inputs[1],inputs[2]);
                }
                else if(request_string.startsWith("FFI")){
                    String input = request_string.substring(3);
                    result = QCStore.findForeignItem(input);
                }
                else if(request_string.startsWith("RFI")){
                    String[] inputs = request_string.substring(3).split(",");
                    result = QCStore.returnForeignItem(inputs[0],inputs[1],inputs[2]);
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
