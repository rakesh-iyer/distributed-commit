import java.net.*;

class Participant {
    public static void main(String args[]) {
        Participant p = new Participant();

        // find other participants.
        DatagramSocket s = new DatagramSocket();
        DatagramPacket p = new DatagramPacket();

        s.bind();
        
    }
