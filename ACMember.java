import java.util.*;
import java.util.concurrent.*;

class ACMember implements Runnable {
    Map<String, ACMemberTransaction> transactionMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    FileLogImpl logImpl;
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
        logImpl = new FileLogImpl("DistributedTransactionLogMember-" + port);

        messageReceiver = new MessageReceiver(messageQueue, port);
        new Thread(messageReceiver).start();
    }

    LogImpl getLogImpl() {
        return logImpl;
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

        try {
            messageQueue.put(actsm);
            sendToPeers(actsm);
            sendToCoordinator(actsm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendToHost(Message m, int port) {
        m.setSenderPort(this.port);
        MessageSender.send(m, port);
    }

    void sendToCoordinator(Message m) {
        m.setSenderPort(port);
        MessageSender.send(m, coordinatorPort);
    }

    void sendToPeers(Message m) {
        m.setSenderPort(port);
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
        List<LogRecord> list = logImpl.readAllRecords();
        Set<LogRecord> transactions = new HashSet<>();

        /*
         * These are transactions that have been locally decided to commit but need consensus of commit/abort.
         */

        for (LogRecord r : list) {
            String tid = r.getTransactionId();

            if (r instanceof StatusRecord) {
                transactions.remove(r);
            } else {
                transactions.add(r);
            }
        }

        System.out.println("These are the transactions that need aborting -");
        for (LogRecord r : transactions) {
            System.out.println(r.getTransactionId());
        }
    }
}
