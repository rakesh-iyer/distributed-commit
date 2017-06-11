import java.util.*;
import java.util.concurrent.*;

class ACMember implements Runnable {
    Map <String, Boolean> transactionStatusMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;
    ACCoordinator coordinator;
    Random r = new Random();

    BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    BlockingQueue<Message> coordinatorMessageQueue = new LinkedBlockingQueue<>();
    final MessageReceiver messageReceiver;
    final int port;
    int peerPorts[];

    ACMember(int port, int peerPorts[], boolean isCoordinator) {
        this.port = port;
        this.peerPorts = peerPorts;
        if (isCoordinator) {
            int [] memberPorts = new int [peerPorts.length + 1];
            memberPorts[0] = port;
            for (int i = 1; i < this.peerPorts.length; i++) {
                memberPorts[i] = peerPorts[i-1];
            }
            this.coordinator = new ACCoordinator(coordinatorMessageQueue, memberPorts);
        }
        messageReceiver = new MessageReceiver(messageQueue, port);
        new Thread(messageReceiver).start();
        new Thread(coordinator).start();
    }

    void addTransaction() {
        String tid = UUID.randomUUID().toString();

        ACTStartMessage actsm = new ACTStartMessage();
        actsm.setTransactionId(tid);
        actsm.setSenderPort(port);

        try {
            messageQueue.put(actsm);
            sendToPeers(actsm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    boolean isCoordinator() {
        return coordinator != null;
    }

    void forward_to_coordinator(Message m) throws InterruptedException{
        coordinatorMessageQueue.put(m);
    }

    void process_t_start(Message m) {
        ACTStartMessage acm = (ACTStartMessage)m;
        String tid = acm.getTransactionId();

        // Just get a pseudo-random commit/abort status.
        boolean commit = getRandomCommitStatus();

        if (transactionStatusMap.get(tid) != null) {
            System.out.println("duplicate message for tid - " + tid);
            return;
        }

        transactionStatusMap.put(tid, commit);
    }

    void process_t_vote(Message m) {
        ACTVoteMessage acvm = (ACTVoteMessage)m;
        int coordinatorPort = acvm.getSenderPort();
        String tid = acvm.getTransactionId();

        if (transactionStatusMap.get(tid) == null) {
            // we got a vote message before start message, do we wait for vote to arrive?
            return;
        }

        ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

        acvrm.setCommited(transactionStatusMap.get(tid));
        acvrm.setSenderPort(port);

        MessageSender.send(acvrm, coordinatorPort);
    }

    void process_t_decision(Message m) {
        ACTDecisionMessage actdm = (ACTDecisionMessage) m;
        String tid = actdm.getTransactionId();

        if (actdm.isCommited()) {
            commitedTransactions.add(tid);
        } else {
            abortedTransactions.add(tid);
        }
    }

    void process() {
        try {
            while (!stopThread) {
                Message m = messageQueue.take();

                System.out.println("Got a message " + m);

                if (m.getType().equals("AC_T_START")) {
                    process_t_start(m);
                    if (isCoordinator()) {
                        forward_to_coordinator(m);
                    }
                } else if (m.getType().equals("AC_T_VOTE")) {
                    process_t_vote(m);
                } else if (m.getType().equals("AC_T_DECISION")) {
                    process_t_decision(m);
                } else if (m.getType().equals("AC_T_VOTE_RESPONSE")) {
                    if (isCoordinator()) {
                        forward_to_coordinator(m);
                    }
                } else {
                    System.out.println("Whats this message about - " + m);
                }
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
