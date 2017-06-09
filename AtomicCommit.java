import java.net.*;
import java.util.*;
import java.io.*;

class AtomicCommit {
    public static void main(String args[]) {
        int port;
        int peerPorts[];
        try {
            int peerCount = args.length - 1;

            port = Integer.valueOf(args[0]);
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

    }
}
