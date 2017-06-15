class ACCoordinatorTransaction {
    ACCoordinator coordinator;
    LinkedQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;
    Map<Integer, Boolean> voteMap = new HashMap<>();

    ACCoordinatorTransaction(ACMember member, String tid) {
        this.member = member;
        this.tid = tid;

        messageQueue = new BlockingQueue<>();
        status = new TransactionStatus();
    }

    void process_t_start(ACTStartMessage m) throws InterruptedException {
        // start phase 1.
        ACTVoteMessage atvm = new ACTVoteMessage();
        atvm.setTransactionId(tid);
        atvm.setSenderPort(memberPorts[0]);

        sendToMembers(atvm);
    }

    boolean allVotesReceived(String tid) {
        for (int port : memberPorts) {
            if (!voteMap.containsKey(port)) {
                return false;
            }
        }

        return true;
    }

    boolean getCommitDecision(String tid) {
        for (Boolean vote : voteMap.values()) {
            if (!vote) {
                return false;
            }
        }

        return true;
    }

    void process_t_vote_response(ACTVoteResponseMessage m) {
        Integer senderPort = actvm.getSenderPort();
        Boolean commit = actvm.isCommited();

        voteMap.put(senderPort, commit);

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
}
