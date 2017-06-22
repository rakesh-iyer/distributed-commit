import java.util.*;

abstract class TwoPCState extends State {
    static final TwoPCAbortState twoPCAbortState = new TwoPCAbortState();
    static final TwoPCCommitState twoPCCommitState = new TwoPCCommitState();
    static final TwoPCUndeterminedState twoPCUndeterminedState = new TwoPCUndeterminedState();

    TwoPCState() {
        setMessageDelay(getMaxMessageDelay());
    }

    LogRecord writeStatusRecord(boolean commit) {
        StatusRecord record = new StatusRecord(null);

        record.setCommited(commit);

        return record;
    }

    long getMaxMessageDelay() {
        return 100; //100 ms.
    }

    boolean isThreePhaseCommit() {
        return true;
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
        if (isThreePhaseCommit()) {
            setTimeoutState(twoPCAbortState);
            setFailureState(twoPCAbortState);
        } else {
            setTimeoutState(twoPCUndeterminedState);
            setFailureState(twoPCUndeterminedState);
        }
        setExpectedMessageType("AC_T_DECISION");
    }

    Message setupDecisionAck(String tid) {
        ACTDecisionAckMessage acdam = new ACTDecisionAckMessage();

        acdam.setTransactionId(tid);

        return acdam;
    }

    StateMessageTuple process(Message m) {
        ACTDecisionMessage actdm = (ACTDecisionMessage)m;
        boolean commit = actdm.isCommited();

        if (commit) {
            Message decisionAckMessage = null;
            if (isThreePhaseCommit()) {
                return new StateMessageTuple(twoPCCommitState, setupDecisionAck(actdm.getTransactionId()), StateMessageTuple.MessageType.RESPONSE, writeStatusRecord(true));
            } else {
                return new StateMessageTuple(twoPCCommitState, null, null, writeStatusRecord(true));
            }
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
    // Need to run a termination protocol to find what happened to this transaction.
    // This will require communicating with other parties that know what happened with the transaction and is inherently blocking.
    StateMessageTuple process(Message m) {
        return null;
    }
}

class TwoPCCoordinatorStartState extends TwoPCState {
    int [] memberPorts;

    TwoPCCoordinatorStartState(int [] memberPorts) {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_START");
        this.memberPorts = memberPorts;
    }

    Message setupVoteRequest(String tid) {
        ACTVoteMessage atvm = new ACTVoteMessage();
        atvm.setTransactionId(tid);

        return atvm;
    }

    StateMessageTuple process(Message m) {
        ACTStartMessage acsm = (ACTStartMessage)m;

        return new StateMessageTuple(new TwoPCCoordinatorWaitForVoteResponseState(memberPorts), setupVoteRequest(acsm.getTransactionId()), StateMessageTuple.MessageType.BROADCAST, null);
    }
}

class TwoPCCoordinatorWaitForVoteResponseState extends TwoPCState {
    int [] memberPorts;
    long startMillis = System.currentTimeMillis();

    TwoPCCoordinatorWaitForVoteResponseState(int [] memberPorts) {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCAbortState);
        setExpectedMessageType("AC_T_VOTE_RESPONSE");
        this.memberPorts = memberPorts;
    }

    Map<Integer, Boolean> voteMap = new HashMap<>();

    boolean allVotesReceived() {
        for (int port : memberPorts) {
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
            // update with remaining message delay.
            setMessageDelay(getMaxMessageDelay() - (System.currentTimeMillis() - startMillis));

            return new StateMessageTuple(this, null, null, null);
        } else {
            // start phase 2.
            commit = getCommitDecision();

            m = setupCommitDecision(tid, commit);

            if (commit) {
                if (isThreePhaseCommit()) {
                    return new StateMessageTuple(new ThreePCCoordinatorWaitForDecisionAckState(memberPorts), m, StateMessageTuple.MessageType.BROADCAST, null);
                } else {
                    return new StateMessageTuple(twoPCCommitState, m, StateMessageTuple.MessageType.BROADCAST, null);
                }
            } else {
                return new StateMessageTuple(twoPCAbortState, m, StateMessageTuple.MessageType.BROADCAST, null);
            }
        }
    }
}

class ThreePCCoordinatorWaitForDecisionAckState extends TwoPCState {
    int [] memberPorts;
    long startMillis = System.currentTimeMillis();

    ThreePCCoordinatorWaitForDecisionAckState(int [] memberPorts) {
        setTimeoutState(twoPCAbortState);
        setFailureState(twoPCCommitState);
        setExpectedMessageType("AC_T_DECISION_ACK");
        this.memberPorts = memberPorts;
    }

    Map<Integer, Boolean> ackMap = new HashMap<>();

    boolean allAcksReceived() {
        for (int port : memberPorts) {
            if (!ackMap.containsKey(port)) {
                return false;
            }
        }

        return true;
    }

    StateMessageTuple process(Message m) {
        ACTDecisionAckMessage actvm = (ACTDecisionAckMessage)m;
        Integer senderPort = actvm.getSenderPort();

        ackMap.put(senderPort, true);

        if (!allAcksReceived()) {
            // update with remaining message delay.
            setMessageDelay(getMaxMessageDelay() - (System.currentTimeMillis() - startMillis));

            return new StateMessageTuple(this, null, null, null);
        } else {
            return new StateMessageTuple(twoPCCommitState, null, null, null);
        }
    }
}
