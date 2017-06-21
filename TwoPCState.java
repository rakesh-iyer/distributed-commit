import java.util.*;

abstract class TwoPCState extends State {
    static final TwoPCMemberAbortState TwoPCMemberAbortState = new TwoPCMemberAbortState();
    static final TwoPCMemberCommitState TwoPCMemberCommitState = new TwoPCMemberCommitState();

    LogRecord writeStatusRecord(boolean commit) {
        StatusRecord record = new StatusRecord(null);

        record.setCommited(commit);

        return record;
    }
}

class TwoPCMemberStartState extends TwoPCState {
    Random r = new Random();

    TwoPCMemberStartState() {
        setTimeoutState(TwoPCMemberAbortState);
        setFailureState(TwoPCMemberAbortState);
        setExpectedMessageType("AC_T_START");
    }

    // pseudo probability of 0.1 returning false.
    boolean getRandomCommitStatus() {
        return r.nextInt(11) < 10;
    }

    StateMessageTuple process(Message m) {
        ACTStartMessage acsm = (ACTStartMessage)m;

        // Just get a pseudo-random commit/abort status with abort probability 1/10.
        boolean commit = getRandomCommitStatus();

        return new StateMessageTuple(new TwoPCMemberWaitForVoteState(commit), null, null, null);
    }
}

class TwoPCMemberWaitForVoteState extends TwoPCState {
    boolean localCommit;

    TwoPCMemberWaitForVoteState(boolean localCommit) {
        setTimeoutState(TwoPCMemberAbortState);
        setFailureState(TwoPCMemberAbortState);
        setExpectedMessageType("AC_T_VOTE");

        this.localCommit = localCommit;
    }

    Message setupVoteResponse(String tid) {
        ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

        acvrm.setCommited(localCommit);
        acvrm.setTransactionId(tid);

        return acvrm;
    }

    StateMessageTuple process(Message m) {
        ACMessage acm = (ACMessage)m;

        if (localCommit) {
            return new StateMessageTuple(new TwoPCMemberWaitForDecisionState(), setupVoteResponse(acm.getTransactionId()), StateMessageTuple.MessageType.RESPONSE, null);
        } else {
            return new StateMessageTuple(new TwoPCMemberAbortState(), null, null, writeStatusRecord(false));
        }
    }
}

class TwoPCMemberWaitForDecisionState extends TwoPCState {
    TwoPCMemberWaitForDecisionState() {
//        setTimeoutState();
//        setFailureState();
        setExpectedMessageType("AC_T_DECISION");
    }

    StateMessageTuple process(Message m) {
        ACTDecisionMessage actdm = (ACTDecisionMessage)m;
        boolean commit = actdm.isCommited();

        if (commit) {
//            member.commitTransaction(tid);
            return new StateMessageTuple(new TwoPCMemberCommitState(), null, null, writeStatusRecord(true));
        } else {
//            member.abortTransaction(tid);
            return new StateMessageTuple(new TwoPCMemberAbortState(), null, null, writeStatusRecord(false));
        }
    }
}

class TwoPCMemberAbortState extends TwoPCState {
    StateMessageTuple process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(false));
    }
} 

class TwoPCMemberCommitState extends TwoPCState {
    StateMessageTuple process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(true));
    }
} 
