import java.net.*;
import java.io.*;

class MessageSender {
   static void send(Message m, int port) {
        try {
            final InetAddress destAddr = InetAddress.getLoopbackAddress();
            InetSocketAddress destSockAddr = new InetSocketAddress(destAddr, port);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(m.getMaxSize());
            ObjectOutputStream oos =  new ObjectOutputStream(baos);

            oos.writeObject(m);

            DatagramSocket s = new DatagramSocket();
            DatagramPacket p = new DatagramPacket(baos.toByteArray(), baos.size(), destSockAddr);

            s.send(p);
            System.out.println("Sent to " + destAddr + " and " +  port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
