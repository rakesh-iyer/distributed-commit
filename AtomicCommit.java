import java.net.*;
import java.util.*;
import java.io.*;

class AtomicCommit {
    public static void main(String args[]) {
        int ports[];
        boolean stopped = false;
        try {
            int portCount = args.length - 1;

            ports = new int[portCount];

            for (int i = 0; i < portCount; i++) {
                ports[i] = Integer.valueOf(args[i]);
            }

            boolean isCoordinator = Boolean.valueOf(args[portCount]);

            if (isCoordinator) {
                int [] memberPorts = Arrays.copyOfRange(ports, 1, portCount);

                ACCoordinator coordinator = new ACCoordinator(ports[0], memberPorts);

                Thread coordinatorThread = new Thread(coordinator);
                coordinatorThread.start();
                coordinatorThread.join();
            } else {
                int [] peerPorts = Arrays.copyOfRange(ports, 2, portCount);

                ACMember member = new ACMember(ports[1], ports[0], peerPorts);

                Thread memberThread = new Thread(member);
                memberThread.start();

                while (!stopped) {
                    Thread.sleep(10000);
                    member.addTransaction();
                }
                memberThread.join();
            }
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
