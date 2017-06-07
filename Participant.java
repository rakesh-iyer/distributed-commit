import java.net.*;
import java.util.*;

class Participant {
    static final Random r = new Random();
    Queue<DatagramPacket> messageQueue = new LinkedList<>();
    Thread messageReceiverThread = new Thread(new MessageReceiver());
    boolean receiverStopped;
    int packetLength = 1000;

    void start() {
        try {
            messageReceiverThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendTestMessage() {
        try {
            final int destPort = 40010;
            final InetAddress destAddr = InetAddress.getLoopbackAddress();
            InetSocketAddress destSockAddr = new InetSocketAddress(destAddr, destPort);

            DatagramSocket s = new DatagramSocket();
            DatagramPacket p = new DatagramPacket(new byte[packetLength], packetLength, destSockAddr);

            System.out.println("Sending to " + destAddr + " and " +  destPort);
            s.send(p);
            System.out.println("Sent to " + destAddr + " and " +  destPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stop() {
        try {
            Thread.sleep(10000);
            System.out.println("Sending a message");
            sendTestMessage();
            receiverStopped = true;
            messageReceiverThread.join();
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

    boolean isValid(DatagramPacket p) {
        return true;
    }

    // abstracts the network reception part.
    class MessageReceiver implements Runnable {
        public void run() {
            try {
                final int port = 40010;
                final InetAddress addr = InetAddress.getLoopbackAddress();
                System.out.println("receiving on " + addr + " and " +  port);
                InetSocketAddress sockAddr = new InetSocketAddress(addr, port);
                DatagramSocket s = new DatagramSocket(sockAddr);

                while (!receiverStopped) {
                    DatagramPacket p = new DatagramPacket(new byte[packetLength], packetLength);
                    s.receive(p);
                    System.out.println("Received a message");

                    if (isValid(p)) {
                        messageQueue.add(p);
                    }
                }
            } catch (Exception e) {
                    e.printStackTrace();
            }
        }
    }

}
