import java.util.*;

class ACMember implements Runnable {
    Map <String, Boolean> transactionStatusMap = new HashMap<>();
    Map <String, Map <Integer, Boolean>> transactionVoteMap = new HashMap<>();
    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;
    boolean coordinator;
    Random r = new Random();

    Queue<Message> messageQueue = new LinkedList<>();
    final MessageReceiver messageReceiver;
    final int port;
    int peerPorts[];

    ACMember(int port, int peerPorts[], boolean coordinator) {
        this.port = port;
        this.peerPorts = peerPorts;
        this.coordinator = coordinator;
        messageReceiver = new MessageReceiver(messageQueue, port);
    }

    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    boolean isCoordinator() {
        return coordinator;
    }

    boolean allVotesReceived(String tid) {
        Map <Integer, Boolean> votes = transactionVoteMap.get(tid);

        for (int port : peerPorts) {
            if (!votes.containsKey(port)) {
                return false;
            }
        }

        return true;
    }

    boolean getCommitDecision(String tid) {
        Map <Integer, Boolean> votes = transactionVoteMap.get(tid);

        for (Boolean vote : votes.values()) {
            if (!vote) {
                return false;
            }
        }

        return true;
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

        if (isCoordinator()) {
            // start phase 1.
            transactionVoteMap.put(tid, new HashMap<>());

            ACTVoteMessage atvm = new ACTVoteMessage();
            atvm.setTransactionId(tid);

            sendToPeers(atvm);
        }
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

    void process_t_vote_response(Message m) {
        if (!isCoordinator()) {
            System.out.println("Should not be getting this message " + m);
        }

        ACTVoteResponseMessage actvm = (ACTVoteResponseMessage) m;

        String tid = actvm.getTransactionId();
        Integer senderPort = actvm.getSenderPort();
        Boolean commit = actvm.isCommited();

        Map <Integer, Boolean> votes = transactionVoteMap.get(tid);

        if (votes == null) {
            System.out.println("Got a vote for a request never sent. " + tid);
        }

        votes.put(senderPort, commit);

        // if you receive all the votes or any abort go ahead and send abort decision.
        if (allVotesReceived(tid)) {
            ACTDecisionMessage actdm = new ACTDecisionMessage();
            boolean decision = getCommitDecision(tid);

            actdm.setTransactionId(tid);
            actdm.setCommited(decision);

            // start phase 2.
            sendToPeers(actdm);
        }
    }

    void process() {
        while (!stopThread) {
            Message m = messageQueue.remove();

            if (m.getType().equals("AC_T_START")) {
                process_t_start(m);
            } else if (m.getType().equals("AC_T_VOTE")) {
                process_t_vote(m);
            } else if (m.getType().equals("AC_T_DECISION")) {
                process_t_decision(m);
            } else if (m.getType().equals("AC_T_VOTE_RESPONSE")) {
                process_t_vote_response(m);
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
