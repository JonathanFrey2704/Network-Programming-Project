package replica_manager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import store.StoreServer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author DRC
 */
public class ReplicaManager {

    StoreServer qcStore;
    StoreServer onStore;
    StoreServer bcStore;

    DatagramSocket replicaSocket;

    /**
     * Amount of time in ms to resend a message that is thought to have been
     * lost in the network during transmission.
     */
    private static final int resendDelay = 1000;
    private boolean processMessages;
    private int nextProcessID;
    private Queue<String> messageQueue;
    private ArrayList<RM> group;
    private ArrayList<String> storedCommands;
    private boolean willFailure;
    private boolean willCrash;

    public ReplicaManager() throws UnknownHostException {
        this(InetAddress.getLocalHost(), 6789, false, false);
    }

    public ReplicaManager(InetAddress address, int port, boolean willFailure, boolean willCrash) {
        qcStore = new StoreServer("QC", port + 10);
        onStore = new StoreServer("ON", port + 20);
        bcStore = new StoreServer("BC", port + 30);
        try {
            this.replicaSocket = new DatagramSocket(port, address);
        } catch (SocketException ex) {
            System.out.println("Replica Manager cannot start since port is already bound");
            System.exit(1);
        }

        this.willFailure = willFailure;
        this.willCrash = willCrash;
        processMessages = true;
        nextProcessID = 0;
        messageQueue = new LinkedList<>();
        storedCommands = new ArrayList<>();
        group = new ArrayList<>();

        this.addReplicaManagers();
        new Thread(() -> {
            waitForRequest();
        }).start();
        new Thread(() -> {
            while (true) {
                while (messageQueue.isEmpty() || !this.processMessages) {
                    Thread.yield();
                }
                processRequest();
            }
        }).start();
    }

    private void addReplicaManagers() {
        try (Scanner fScn = new Scanner(new File("ReplicaManagers.txt"))) {
            String data;

            while (fScn.hasNextLine()) {
                data = fScn.nextLine();
                if (data.startsWith("//")) {
                    continue;
                }
                String[] token = data.split(",");
                InetAddress address = InetAddress.getByName(token[0]);
                int port = Integer.parseInt(token[1]);
                group.add(new RM(address, port));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Could not add replica managers.");
        } catch (UnknownHostException ex) {
            System.out.println("Could not add replica managers.");
        }
    }

    private RM getReplicaManager(InetAddress address, int port) {
        for (RM rm : group) {
            if (rm.equals(new RM(address, port))) {
                return rm;
            }
        }
        return null;
    }

    /**
     * Waits for UDP requests. When a request is received, it added it to the
     * queue and sends a response that it was received.
     */
    private void waitForRequest() {
        try {
            byte[] buffer = new byte[1000];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            replicaSocket.receive(request);
            String message = new String(request.getData()).trim();
            System.out.println(message);
            this.sendReceivedMessage(request.getAddress(), request.getPort());
            new Thread(() -> {
                waitForRequest();
            }).start();

            if (message.toUpperCase().contains("ERROR")) {
                String[] args = message.split(";")[1].split(",");
                String addr = args[1].trim();
                String p = args[2].trim();
                RM error = this.getReplicaManager(InetAddress.getByName(addr), Integer.parseInt(p));
                this.handleIncorrectReplica(error);
                return;
            } else if (message.toUpperCase().contains("CRASH")) {
                String[] args = message.split(";")[1].split(",");
                String addr = args[1].trim();
                String p = args[2].trim();
                RM crashed = this.getReplicaManager(InetAddress.getByName(addr), Integer.parseInt(p));
                this.handleCrashedReplica(crashed);
                return;
            } else if (message.toUpperCase().contains("PAUSE_MESSAGES")) {
                this.processMessages = false;
                return;
            } else if (message.toUpperCase().contains("RESUME_MESSAGES")) {
                this.processMessages = true;
                return;
            }
            messageQueue.add(message);
        } catch (SocketException ex) {
            System.out.println("Could not connect to port, canceling server");
            System.exit(1);
        } catch (IOException ex) {
        }
    }

    /**
     * Gets the next message in the queue and processes it. If the queue is
     * empty nothing is processed. If the message was already processed, meaning
     * the sequencer ID is less than the next ID to be processed, then the
     * message is not processed. If the message is too early, meaning the
     * sequencer ID is greater than the next id to be processed, then add it
     * back to the queue.
     */
    private void processRequest() {
        try {
            String message = messageQueue.poll();
            if (message == null) {
                return;
            }
            String[] splitMessage = message.split(";");

            String sender = splitMessage[0];
            String[] args = sender.split(",");
            InetAddress senderAddress = InetAddress.getByName(args[0]);
            int senderPort = Integer.parseInt(args[1]);
            int processID = Integer.parseInt(args[2]);
            
            // Ensures that no duplicate message is processed.
            if (processID < this.nextProcessID) {
                return;
            } else if (processID > this.nextProcessID) {
                messageQueue.add(message);
                return;
            }

            String query = splitMessage[1];
            storedCommands.add(query);
            String result = this.callMethod(query);
            if (!willCrash) {
                this.sendAnswerMessage(result, senderAddress, senderPort);
            }
            this.nextProcessID++;
        } catch (IOException ex) {
        }
    }

    private String callMethod(String methodSignature) {

        String[] args = methodSignature.split(",");
        StoreServer usersStore = null;
        if (args[1].toUpperCase().contains("QC")) {
            usersStore = qcStore;
        } else if (args[1].toUpperCase().contains("ON")) {
            usersStore = onStore;
        } else if (args[1].toUpperCase().contains("BC")) {
            usersStore = bcStore;
        } else {
            return ";ERROR:NOT A VALID STORE";
        }

        String result;
        switch (args[0].trim().toUpperCase()) {
            case "ADD":
                result = usersStore.addItem(args[1].trim(), args[2].trim(), args[3].trim(), Integer.parseInt(args[4].trim()), Double.parseDouble(args[5].trim()));
                break;
            case "REMOVE":
        		result = usersStore.removeItem(args[1].trim(), args[2].trim(), Integer.parseInt(args[3].trim()));
                break;
            case "LIST":
				if (willFailure) {
                    result = "ERROR";
           	 	} else {
           	 		String[] results = usersStore.listItemAvailability(args[1].trim());
           	 		result = Arrays.toString(results);
	           	 }
                break;
            case "PURCHASE":
                result = usersStore.purchaseItem(args[1].trim(), args[2].trim(), args[3].trim());
                break;
            case "FIND":
                String[] results = usersStore.findItem(args[1].trim(), args[2].trim());
                result = Arrays.toString(results);
                break;
            case "RETURN":
                result = usersStore.returnItem(args[1].trim(), args[2].trim(), args[3].trim());
                break;
            case "EXCHANGE":
                result = usersStore.exchangeItem(args[1].trim(), args[2].trim(), args[3].trim(), args[4].trim());
                break;
            default:
                result = "ERROR:NOT A VALID METHOD";
        }
        return result;
    }

    /**
     * Sends a received message to a process.
     *
     * @param requestAddress the InetAdress of the machine
     * @param requestPort the port to connect to
     */
    private void sendReceivedMessage(InetAddress requestAddress, int requestPort) {
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
    private void sendAnswerMessage(String message, InetAddress requestAddress, int requestPort) {

        boolean not_received = true;
        String header = String.format("%s,%d;", replicaSocket.getLocalAddress().getHostAddress(), replicaSocket.getLocalPort());
        message = header + message;
        byte[] resultBytes = message.getBytes();
        DatagramPacket request = new DatagramPacket(resultBytes, resultBytes.length, requestAddress, requestPort);
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            sendSocket.setSoTimeout(ReplicaManager.resendDelay);
            while (not_received) {
                sendSocket.send(request);
                System.out.println("Sent reply");
                try {
                    byte[] buffer = new byte[1000];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    sendSocket.receive(reply);
                    String answer = new String(reply.getData()).trim();
                    if (answer.toUpperCase().equals("RECEIVED")) {
                        not_received = false;
                    }
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (IOException ex) {
        }
    }

    private void sendPauseProcessing() {
        for (RM rm : group) {
            sendAnswerMessage("PAUSE_MESSAGES", rm.address, rm.port);
        }
    }

    private void sendResumeProcessing() {
        for (RM rm : group) {
            sendAnswerMessage("RESUME_MESSAGES", rm.address, rm.port);
        }
    }

    private void handleIncorrectReplica(RM errorRM) {
        errorRM.incorrectProcesses += 1;
        if (errorRM.incorrectProcesses % 3 == 0) {
            RM thisRM = this.getReplicaManager(this.replicaSocket.getLocalAddress(), this.replicaSocket.getLocalPort());
            if (thisRM.equals(errorRM)) {
                this.restartReplicas();
            }
        }
    }

    private void handleCrashedReplica(RM crashed) {
        RM thisRM = this.getReplicaManager(this.replicaSocket.getLocalAddress(), this.replicaSocket.getLocalPort());
        if (thisRM.equals(crashed)) {
            this.restartReplicas();
        }
    }

    private void restartReplicas() {
        System.out.println("restarting");
        sendPauseProcessing();
        
        this.willCrash = false;
        this.willFailure = false;
        
        qcStore.close();
        onStore.close();
        bcStore.close();
        qcStore = new StoreServer("QC", 5461);
        onStore = new StoreServer("ON", 5462);
        bcStore = new StoreServer("BC", 5463);
        for (String query : storedCommands) {
            this.callMethod(query);
        }
        
        sendResumeProcessing();
        System.out.println("complete");
    }

    private static class RM {

        InetAddress address;
        int port;
        int incorrectProcesses;
        long heartBeats;

        public RM(InetAddress address, int port) {
            this.address = address;
            this.port = port;
            this.incorrectProcesses = 0;
            this.heartBeats = 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RM)) {
                return false;
            }
            RM other = (RM) obj;
            return this.address.equals(other.address) && this.port == other.port;
        }
    }

}
