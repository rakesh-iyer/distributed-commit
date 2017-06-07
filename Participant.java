import java.net.*;
import java.util.*;
import java.io.*;

class Participant {
    static final Random r = new Random();
    Queue<Message> messageQueue = new LinkedList<>();
    final int messageReceiverPort;
    final MessageReceiver messageReceiver;
    boolean receiverStopped;
    int packetLength = 1000;
    int peerPorts[];

    Participant(int messageReceiverPort, int peerPorts[]) {
        this.messageReceiverPort = messageReceiverPort;
        this.peerPorts = peerPorts;
        messageReceiver = new MessageReceiver(messageQueue, messageReceiverPort);
    }

    void start() {
        try {
            messageReceiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    void sendTestMessagesToPeers() {
        System.out.println("Sending a message to peers");

        Thread.sleep(10000);
        Message m = new Message();
        m.setType("T_START");
        m.setData("T Details");
        while(true) {
            sendToPeers(m);
            Thread.sleep(1000);
        }
    }

    void stop() {
        try {

//            messageReceiver.halt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        int messageReceiverPort;
        int peerPorts[];
        try {
            int peerCount = args.length - 1;

            messageReceiverPort = Integer.valueOf(args[0]);
            peerPorts = new int[peerCount];

            for (int i = 0; i < peerCount; i++) {
                peerPorts[i] = Integer.valueOf(args[i+1]);
            }

        } catch (NumberFormatException e) {
            System.out.println("Bad port number or peer port number");
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Did not provide port number");
            return;
        }

        Participant p = new Participant(messageReceiverPort, peerPorts);

        p.start();
        p.stop();
        // find other participants.
    }

    boolean isValid(Message m) {
        return true;
    }


}
