import java.util.*;

class ACCoordinator implements Runnable {
    Map <String, Map<Integer, Boolean>> transactionVoteMap = new HashMap<>();
    boolean stopThread;

    Queue<Message> messageQueue = new LinkedList<>();
    final MessageReceiver messageReceiver;
    final int port;
    int peerPorts[];

    ACCoordinator(int port, int peerPorts[]) {
        this.port = port;
        this.peerPorts = peerPorts;
        messageReceiver = new MessageReceiver(messageQueue, port);
    }

    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
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

    void process() {
        while (!stopThread) {
        Message m = messageQueue.remove();

        if (m.getType().equals("AC_T_START")) {
            ACTStartMessage acm = (ACTStartMessage)m;
            String tid = acm.getTransactionId();

            transactionVoteMap.put(tid, new HashMap<>());

            //start phase 1
            ACTVoteMessage atvm = new ACTVoteMessage();
            atvm.setTransactionId(tid);

            sendToPeers(atvm);
        } else if (m.getType().equals("AC_T_VOTE_RESPONSE")) {
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
        } else {
            System.out.println("Whats this message about - " + m);
        }
        }
    }

    public void run() {
        process();
    }
}
