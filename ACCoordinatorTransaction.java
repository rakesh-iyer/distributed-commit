import java.util.*;
import java.util.concurrent.*;

class ACCoordinatorTransaction implements Runnable {
    ACCoordinator coordinator;
    BlockingQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;
    Map<Integer, Boolean> voteMap = new HashMap<>();
    long messageDelay = 100;
    boolean stopThread;

    ACCoordinatorTransaction(ACCoordinator coordinator, String tid) {
        this.coordinator = coordinator;
        this.tid = tid;

        messageQueue = new LinkedBlockingQueue<>();
        status = new ACTransactionStatus();
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    void process_t_start(ACTStartMessage acsm) throws InterruptedException {
        // start phase 1.
        ACTVoteMessage atvm = new ACTVoteMessage();
        atvm.setTransactionId(tid);
        atvm.setSenderPort(coordinator.getPort());

        coordinator.sendToMembers(atvm);
    }

    boolean allVotesReceived() {
        for (int port : coordinator.getMemberPorts()) {
            if (!voteMap.containsKey(port)) {
                return false;
            }
        }

        return true;
    }

    boolean getCommitDecision() {
        for (Boolean vote : voteMap.values()) {
            if (!vote) {
                return false;
            }
        }

        return true;
    }

    void process_t_vote_response(ACTVoteResponseMessage actvm) {
        Integer senderPort = actvm.getSenderPort();
        Boolean commit = actvm.isCommited();

        voteMap.put(senderPort, commit);

        // if you receive all the votes or any abort go ahead and send abort decision.
        if (allVotesReceived()) {
            ACTDecisionMessage actdm = new ACTDecisionMessage();
            boolean decision = getCommitDecision();

            actdm.setTransactionId(tid);
            actdm.setCommited(decision);
            actdm.setSenderPort(coordinator.getPort());

            // start phase 2.
            coordinator.sendToMembers(actdm);
        }
    }

    void process() {
        try {
            Message m;

            m = TimedMessage.get_message_type(messageQueue, "AC_T_START", messageDelay);
            process_t_start((ACTStartMessage)m);

            long startMillis = System.currentTimeMillis();
            while (!allVotesReceived()) {
                long sleepTime = messageDelay - (System.currentTimeMillis() - startMillis);

                m = TimedMessage.get_message_type(messageQueue, "AC_T_VOTE_RESPONSE", sleepTime);
                process_t_vote_response((ACTVoteResponseMessage)m);
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        process();
    }
}
