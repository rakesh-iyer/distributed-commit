import java.util.*;
import java.util.concurrent.*;

class ACMemberTransaction implements Runnable {
    ACMember member;
    BlockingQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;
    boolean stopThread;
    long messageDelay = 100;
    Random r = new Random();

    ACMemberTransaction(ACMember member, String tid) {
        this.member = member;
        this.tid = tid;

        messageQueue = new LinkedBlockingQueue<>();
        status = new ACTransactionStatus();
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    void process_t_start(ACTStartMessage acsm) {
        // Just get a pseudo-random commit/abort status.
        boolean commit = getRandomCommitStatus();

        status.setCommited(commit);
        status.setStarted(true);

        if (status.isVoting()) {
            sendVoteResponse(tid);
        }
    }

    void process_t_vote(ACTVoteMessage acvm) {
        int coordinatorPort = acvm.getSenderPort();

        status.setVoting(true);

        if (status.isStarted()) {
            sendVoteResponse(tid);
        }
    }

    void sendVoteResponse(String tid) {
        ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

        acvrm.setCommited(status.isCommited());
        acvrm.setTransactionId(tid);
        acvrm.setSenderPort(member.getPort());

        member.sendToCoordinator(acvrm);
    }

    // pseudo probability of 0.1 returning false.
    boolean getRandomCommitStatus() {
        return r.nextInt(11) < 10;
    }

    void process_t_decision(ACTDecisionMessage actdm) {
        if (actdm.isCommited()) {
            member.commitTransaction(tid);
        } else {
            member.abortTransaction(tid);
        }
    }

    void process() {
        try {
            Message m;

            m = TimedMessage.get_message_type(messageQueue, "AC_T_START", messageDelay);
            process_t_start((ACTStartMessage)m);

            m = TimedMessage.get_message_type(messageQueue, "AC_T_VOTE", messageDelay);
            process_t_vote((ACTVoteMessage)m);

            m = TimedMessage.get_message_type(messageQueue, "AC_T_DECISION", messageDelay);
            process_t_decision((ACTDecisionMessage)m);
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

