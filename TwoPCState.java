import java.util.*;

abstract class TwoPCState extends State {
    static final TwoPCAbortState twoPCAbortState = new TwoPCAbortState();
    static final TwoPCCommitState twoPCCommitState = new TwoPCCommitState();
    static final TwoPCUndeterminedState twoPCUndeterminedState = new TwoPCUndeterminedState();

    LogRecord writeStatusRecord(boolean commit) {
        StatusRecord record = new StatusRecord(null);

        record.setCommited(commit);

        return record;
    }
}

class TwoPCMemberStartState extends TwoPCState {
    Random r = new Random();

    TwoPCMemberStartState() {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_START");
    }

    // pseudo probability of 0.1 returning false.
    boolean getRandomCommitStatus() {
        return r.nextInt(11) < 10;
    }

    StateMessageTuple process(Message m) {
        // Just get a pseudo-random commit/abort status with abort probability 1/10.
        boolean commit = getRandomCommitStatus();

        return new StateMessageTuple(new TwoPCMemberWaitForVoteState(commit), null, null, null);
    }
}

class TwoPCMemberWaitForVoteState extends TwoPCState {
    boolean localCommit;

    TwoPCMemberWaitForVoteState(boolean localCommit) {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
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
            return new StateMessageTuple(twoPCAbortState, setupVoteResponse(acm.getTransactionId()), StateMessageTuple.MessageType.RESPONSE, writeStatusRecord(false));
        }
    }
}

class TwoPCMemberWaitForDecisionState extends TwoPCState {
    TwoPCMemberWaitForDecisionState() {
        setTimeoutState(twoPCUndeterminedState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_DECISION");
    }

    StateMessageTuple process(Message m) {
        ACTDecisionMessage actdm = (ACTDecisionMessage)m;
        boolean commit = actdm.isCommited();

        if (commit) {
            return new StateMessageTuple(twoPCCommitState, null, null, writeStatusRecord(true));
        } else {
            return new StateMessageTuple(twoPCAbortState, null, null, writeStatusRecord(false));
        }
    }
}

class TwoPCAbortState extends TwoPCState {
    StateMessageTuple process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(false));
    }
} 

class TwoPCCommitState extends TwoPCState {
    StateMessageTuple process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(true));
    }
} 

class TwoPCUndeterminedState extends TwoPCState {
    StateMessageTuple process(Message m) {
        return null;
    }
}

class TwoPCCoordinatorStartState extends TwoPCState {
    TwoPCCoordinatorStartState() {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_START");
    }

    Message setupVoteRequest(String tid) {
        ACTVoteMessage atvm = new ACTVoteMessage();
        atvm.setTransactionId(tid);

        return atvm;
    }

    StateMessageTuple process(Message m) {
        ACTStartMessage acsm = (ACTStartMessage)m;

        return new StateMessageTuple(new TwoPCCoordinatorWaitForVoteResponseState(), setupVoteRequest(acsm.getTransactionId()), StateMessageTuple.MessageType.BROADCAST, null);
    }
}

class TwoPCCoordinatorWaitForVoteResponseState extends TwoPCState {
    TwoPCCoordinatorWaitForVoteResponseState() {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_VOTE_RESPONSE");
    }

    Map<Integer, Boolean> voteMap = new HashMap<>();

    boolean allVotesReceived() {
/*        for (int port : coordinator.getMemberPorts()) {
            if (!voteMap.containsKey(port)) {
                return false;
            }
        }
*/
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

    Message setupCommitDecision(String tid, boolean decision) {
        ACTDecisionMessage actdm = new ACTDecisionMessage();

        actdm.setTransactionId(tid);
        actdm.setCommited(decision);

        return actdm;
    }

    StateMessageTuple process(Message m) {
        ACTVoteResponseMessage actvm = (ACTVoteResponseMessage)m;
        String tid = actvm.getTransactionId();
        Integer senderPort = actvm.getSenderPort();
        Boolean commit = actvm.isCommited();

        voteMap.put(senderPort, commit);

        if (!allVotesReceived()) {
            return new StateMessageTuple(this, null, null, null);
        } else {
            // start phase 2.
            commit = getCommitDecision();

            m = setupCommitDecision(tid, commit);

            if (commit) {
                return new StateMessageTuple(twoPCCommitState, m, StateMessageTuple.MessageType.BROADCAST, null);
            } else {
                return new StateMessageTuple(twoPCAbortState, m, StateMessageTuple.MessageType.BROADCAST, null);
            }
        }
    }
}
