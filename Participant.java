import java.net.*;
import java.util.*;
import java.io.*;

class Participant {
    static final Random r = new Random();
    Queue<Message> messageQueue = new LinkedList<>();
    final int messageReceiverPort = r.nextInt(65535);
    MessageReceiver messageReceiver = new MessageReceiver(messageQueue, messageReceiverPort);
    boolean receiverStopped;
    int packetLength = 1000;

    void start() {
        try {
            messageReceiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendTestMessage() {
        try {
            final InetAddress destAddr = InetAddress.getLoopbackAddress();
            InetSocketAddress destSockAddr = new InetSocketAddress(destAddr, messageReceiverPort);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(packetLength);
            ObjectOutputStream oos =  new ObjectOutputStream(baos);

            Message m = new Message();
            m.setType("T_START");
            m.setData("T Details");

            oos.writeObject(m);

            DatagramSocket s = new DatagramSocket();
            DatagramPacket p = new DatagramPacket(baos.toByteArray(), baos.size(), destSockAddr);

            System.out.println("Sending to " + destAddr + " and " +  messageReceiverPort);
            s.send(p);
            System.out.println("Sent to " + destAddr + " and " +  messageReceiverPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stop() {
        try {
            System.out.println("Sending a message");
            sendTestMessage();
            messageReceiver.halt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Participant p = new Participant();

        p.start();
        p.stop();
        // find other participants.
    }

    boolean isValid(Message m) {
        return true;
    }


}
