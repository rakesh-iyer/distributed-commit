import java.util.*;
import java.util.concurrent.*;

class ACMember implements Runnable {
    Map<String, Transaction> transactionMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;
    Random r = new Random();

    BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    final MessageReceiver messageReceiver;
    final int port;
    int peerPorts[];

    ACMember(int port, int coordinatorPort, int[] peerPorts) {
        this.port = port;
        this.coordinatorPort = pcoordinatorPort;
        this.peerPorts = peerPorts;

        messageReceiver = new MessageReceiver(messageQueue, port);
        new Thread(messageReceiver).start();
    }

    void commitTransaction(ACMemberTransaction t) {
        commitedTransactions.add(t);
    }

    void abortTransaction(ACMemberTransaction t) {
        abortedTransactions.add(t);
    }

    void addTransaction() {
        String tid = UUID.randomUUID().toString();

        ACTStartMessage actsm = new ACTStartMessage();
        actsm.setTransactionId(tid);
        actsm.setSenderPort(port);

        try {
            messageQueue.put(actsm);
            sendToPeers(actsm);
            sendToCoordinator(actsm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendToCoordinator(Message m) {
        MessageSender.send(m, coordinatorPort);
    }

    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    void process() {
        try {
            while (!stopThread) {
                ACMessage acm = (ACMessage)messageQueue.take();
                String tid = acm.getTransactionId();

                System.out.println("Got a message " + acm);

                if (acm.getType().equals("AC_T_START")) {
                    ACMemberTransaction t =  new ACMemberTransaction(this, tid);
                    transactionMap.add(t);
                    new Thread(t).start();
                }

                ACMemberTransaction t = transactionMap.get(tid);
                t.getMessageQueue().put(acm);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // pseudo probability of 0.1 returning false.
    boolean getRandomCommitStatus() {
        return r.nextInt(11) < 10;
    }

    public void run() {
        process();
    }
}
