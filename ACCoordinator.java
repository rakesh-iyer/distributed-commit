import java.util.*;
import java.util.concurrent.*;

class ACCoordinator implements Runnable {
    // this is a local producer/consumer.
    BlockingQueue<Message> messageQueue;
    Map <String, Map <Integer, Boolean>> transactionVoteMap = new HashMap<>();
    int memberPorts[];
    boolean stoppedThread;
    static final int messageDelayMillis = 20;

    ACCoordinator(BlockingQueue<Message> messageQueue, int memberPorts[]) {
        this.messageQueue = messageQueue;
        this.memberPorts = memberPorts;
    }

    void sendToMembers(Message m) {
        for (int port : memberPorts) {
            System.out.println(port);
            MessageSender.send(m, port);
        }
    }

    boolean allVotesReceived(String tid) {
        Map <Integer, Boolean> votes = transactionVoteMap.get(tid);

        for (int port : memberPorts) {
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

    void wait_message_delay() throws InterruptedException {
        Thread.sleep(messageDelayMillis);
    }

    void process_t_start(Message m) throws InterruptedException {
        ACTStartMessage acm = (ACTStartMessage)m;
        String tid = acm.getTransactionId();

        // start phase 1.
        transactionVoteMap.put(tid, new HashMap<>());

        ACTVoteMessage atvm = new ACTVoteMessage();
        atvm.setTransactionId(tid);
        atvm.setSenderPort(memberPorts[0]);

        sendToMembers(atvm);
    }

    void process_t_vote_response(Message m) {
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
            actdm.setSenderPort(memberPorts[0]);

            // start phase 2.
            sendToMembers(actdm);
        }
    }

    void process() {
        try {
            while (!stoppedThread) {
                Message m = messageQueue.take();

                if (m.getType().equals("AC_T_START")) {
                    process_t_start(m);
                } else if (m.getType().equals("AC_T_VOTE_RESPONSE")) {
                    process_t_vote_response(m);
                } else {
                    System.out.println("Unexpected message for coordinator - " + m);
                }
            }            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        process();
    }
}
