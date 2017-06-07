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

    Participant(int messageReceiverPort) {
        this.messageReceiverPort = messageReceiverPort;
        messageReceiver = new MessageReceiver(messageQueue, messageReceiverPort);
    }

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
        int messageReceiverPort;

        try {
            messageReceiverPort = Integer.valueOf(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Bad port number");
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Did not provide port number");
            return;
        }

        Participant p = new Participant(messageReceiverPort);

        p.start();
        p.stop();
        // find other participants.
    }

    boolean isValid(Message m) {
        return true;
    }


}
