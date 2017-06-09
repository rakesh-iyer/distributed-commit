import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

// abstracts the network reception part.
// extending thread so start seems intuitive. Unfortunately need to use halt method instead of stop as stop is deprecated.
class MessageReceiver extends Thread {
    int port;
    Thread thread;
    BlockingQueue<Message> messageQueue;
    final InetAddress addr = InetAddress.getLoopbackAddress();
    boolean receiverStopped;

    MessageReceiver(BlockingQueue<Message> messageQueue, int port) {
        this.messageQueue = messageQueue;
        this.port = port;
    }

    public void halt() {
        try {
            receiverStopped = true;
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            thread = Thread.currentThread();
            System.out.println("receiving on " + addr + " and " +  port);

            InetSocketAddress sockAddr = new InetSocketAddress(addr, port);
            DatagramSocket s = new DatagramSocket(sockAddr);

            while (!receiverStopped) {
                DatagramPacket p = new DatagramPacket(new byte[Message.getMaxSize()], Message.getMaxSize());
                s.receive(p);

                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Message m = (Message) ois.readObject();

                if (m.isValid()) {
                    messageQueue.put(m);
                }
                System.out.println("Received a message - " + m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
