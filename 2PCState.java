abstract class 2PCState extends State {
    LogRecord writeStatusRecord(boolean commit) {
        StatusRecord record = new StatusRecord(null);

        record.setCommited(commit);
    }
}

class 2PCMemberStartState extends 2PCState {
    2PCMemberStartState() {
        setTimeoutState(2PCAbortState);
        setFailureState(2PCAbortState);
        setExpectedMessageType("AC_T_START");
    }

    // pseudo probability of 0.1 returning false.
    boolean getRandomCommitStatus() {
        return r.nextInt(11) < 10;
    }

    State process(Message m) {
        ACTStartMessage acsm = (ACTStartMessage)m;

        // Just get a pseudo-random commit/abort status with abort probability 1/10.
        boolean commit = getRandomCommitStatus();

        return new StateMessageTuple(new 2PCMemberWaitForVoteState(commit), null, null, null);
    }
}

class 2PCMemberWaitForVoteState extends 2PCState {
    boolean localCommit;

    2PCMemberWaitForVoteState(boolean localCommit) {
        setTimeoutState(2PCAbortState);
        setFailureState(2PCAbortState);
        setExpectedMessageType("AC_T_VOTE");

        this.localCommit = localCommit;
    }

    Message setupVoteResponse(String tid) {
        ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

        acvrm.setCommited(localCommit);
        acvrm.setTransactionId(tid);

        return acvrm;
    }

    State process(Message m) {
        if (localCommit) {
            return new StateMessageTuple(new 2PCMemberWaitForDecisionState(), setupVoteResponse(tid), StateMessageTuple.MessageType.RESPONSE, null);
        } else {
            return new StateMessageTuple(new 2PCAbortState(), null, null, writeStatusRecord(false));
        }
    }
}

class 2PCMemberWaitForDecisionState extends 2PCState {
    2PCMemberWaitForDecisionState() {
        setTimeoutState();
        setFailureState();
        setExpectedMessageType("AC_T_DECISION");
    }

    State process(Message m) {
        ACTDecisionMessage actdm = (ACTDecisionMessage)m;
        boolean commit = actdm.isCommited();

        if (commit) {
//            member.commitTransaction(tid);
            return new StateMessageTuple(new 2PCMemberCommitState(), null, null, writeStatusRecord(true));
        } else {
//            member.abortTransaction(tid);
            return new StateMessageTuple(new 2PCMemberAbortState(), null, null, writeStatusRecord(false));
        }
    }
}

class 2PCMemberAbortState extends 2PCState {
    State process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(false));
    }
} 

class 2PCMemberCommitState extends 2PCState {
    State process(Message m) {
        // This might be a duplicate log write, but due to its idempotence its fine.
        return new StateMessageTuple(null, null, null, writeStatusRecord(true));
    }
} 
