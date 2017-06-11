import java.net.*;
import java.util.*;
import java.io.*;

class AtomicCommit {
    public static void main(String args[]) {
        int port;
        int peerPorts[];
        boolean stopped = false;
        try {
            int peerCount = args.length - 2;

            port = Integer.valueOf(args[0]);
            peerPorts = new int[peerCount];

            int i = 0;
            for (; i < peerCount; i++) {
                peerPorts[i] = Integer.valueOf(args[i+1]);
            }

            boolean coordinator = Boolean.valueOf(args[i+1]);

            ACMember member = new ACMember(port, peerPorts, coordinator);
            Thread memberThread = new Thread(member);

            memberThread.start();

            if (coordinator) {
                while (!stopped) {
                    Thread.sleep(10000);
                    member.addTransaction();
                }
            }

            memberThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Bad port number or peer port number");
            return;
        } catch (ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
            System.out.println("Did not provide port number");
            return;
        }
    }
}
