import java.util.*;

class ACParticipant implements Runnable {
    Map <String, Boolean> transactionStatusMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;
    Random r = new Random();

    Queue<Message> messageQueue = new LinkedList<>();
    final MessageReceiver messageReceiver;
    final int port;
    int peerPorts[];

    ACParticipant(int port, int peerPorts[]) {
        this.port = port;
        this.peerPorts = peerPorts;
        messageReceiver = new MessageReceiver(messageQueue, port);
    }

    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    void process() {
        while (!stopThread) {
        Message m = messageQueue.remove();

        if (m.getType().equals("AC_T_START")) {
            ACTStartMessage acm = (ACTStartMessage)m;
            String tid = acm.getTransactionId();

            // Just get a pseudo-random commit/abort status.
            boolean commit = getRandomCommitStatus();

            if (transactionStatusMap.get(tid) != null) {
            System.out.println("duplicate message for tid - " + tid);
            continue;
            }

            transactionStatusMap.put(tid, commit);
        } else if (m.getType().equals("AC_T_VOTE")) {
            ACTVoteMessage acvm = (ACTVoteMessage)m;
            int coordinatorPort = acvm.getSenderPort();
            String tid = acvm.getTransactionId();

            if (transactionStatusMap.get(tid) == null) {
            // we got a vote message before start message, do we wait for vote to arrive?
            continue;
            }

            ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

            acvrm.setCommited(transactionStatusMap.get(tid));
            acvrm.setSenderPort(port);

            MessageSender.send(acvrm, coordinatorPort);
        } else if (m.getType().equals("AC_T_DECISION")) {
            ACTDecisionMessage actdm = (ACTDecisionMessage) m;
            String tid = actdm.getTransactionId();

            if (actdm.isCommited()) {
            commitedTransactions.add(tid);
            } else {
            abortedTransactions.add(tid);
            }
        } else {
            System.out.println("Whats this message about - " + m);
        }
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
