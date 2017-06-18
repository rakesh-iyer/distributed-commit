import java.util.*;
import java.util.concurrent.*;

class ACMember implements Runnable {
    Map<String, ACMemberTransaction> transactionMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;
    Random r = new Random();

    BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    final MessageReceiver messageReceiver;
    final int port;
    int coordinatorPort;
    int peerPorts[];
    boolean doRecovery;

    ACMember(int port, int coordinatorPort, int[] peerPorts, boolean doRecovery) {
        this.port = port;
        this.coordinatorPort = coordinatorPort;
        this.peerPorts = peerPorts;
        this.doRecovery = doRecovery;

        messageReceiver = new MessageReceiver(messageQueue, port);
        new Thread(messageReceiver).start();
    }

    int getPort() {
        return port;
    }

    void commitTransaction(String tid) {
        commitedTransactions.add(tid);
    }

    void abortTransaction(String tid) {
        abortedTransactions.add(tid);
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
                    transactionMap.put(tid, t);
                    new Thread(t).start();
                }

                ACMemberTransaction t = transactionMap.get(tid);
                t.getMessageQueue().put(acm);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (doRecovery) {
            recover();
        }

        process();
    }

    public void recover() {
    }
}
