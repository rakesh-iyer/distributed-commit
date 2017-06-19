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

    boolean process_t_start(ACTStartMessage acsm) {
        // Just get a pseudo-random commit/abort status.
        boolean commit = getRandomCommitStatus();

        status.setCommited(commit);
        status.setStarted(true);

        if (status.isVoting()) {
            sendVoteResponse(tid);
        }

        member.getLogImpl().writeRecord(new LogRecord(tid));

        return commit;
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

    void writeStatusRecord(boolean commit) {
        StatusRecord record = new StatusRecord(tid);

        record.setCommited(commit);
        member.getLogImpl().writeRecord(record);
    }

    boolean process_t_decision(ACTDecisionMessage actdm) {
        boolean commit = actdm.isCommited();
        writeStatusRecord(commit);
        if (commit) {
            member.commitTransaction(tid);
        } else {
            member.abortTransaction(tid);
        }

        return commit;
    }

    void process() {
        boolean commit = false;
        try {
            Message m;

            m = TimedMessage.get_message_type(messageQueue, "AC_T_START", messageDelay);
            commit = process_t_start((ACTStartMessage)m);

            m = TimedMessage.get_message_type(messageQueue, "AC_T_VOTE", messageDelay);
            process_t_vote((ACTVoteMessage)m);

            if (commit) {
                m = TimedMessage.get_message_type(messageQueue, "AC_T_DECISION", messageDelay);
                commit = process_t_decision((ACTDecisionMessage)m);
            }
        } catch (TimeoutException e) {
            // abort transaction.
            writeStatusRecord(false);
            member.abortTransaction(tid);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!commit)  {
            writeStatusRecord(false);
            member.abortTransaction(tid);
            return;
        }
    }

    public void run() {
        process();
    }
}

