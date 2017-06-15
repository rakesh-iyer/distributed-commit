class ACMemberTransaction implements Runnable {
    ACMember member;
    LinkedQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;

    ACMemberTransaction(ACMember member, String tid) {
        this.member = member;
        this.tid = tid;

        messageQueue = new BlockingQueue<>();
        status = new TransactionStatus();
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
        status.setCoordinatorPort(coordinatorPort);

        if (status.isStarted()) {
            sendVoteResponse(tid);
        }
    }

    void sendVoteResponse(String tid) {
        ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();
        TransactionStatus status = transactionStatusMap.get(tid);

        acvrm.setCommited(status.isCommited());
        acvrm.setTransactionId(tid);
        acvrm.setSenderPort(port);

        member.sendToCoordinator(acvrm);
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
            while (!stopThread) {
                ACMessage acm = (ACMessage)messageQueue.take();

                System.out.println("Got a message " + acm);

                if (acm.getType().equals("AC_T_START")) {
                    process_t_start(acm);
                } else if (acm.getType().equals("AC_T_VOTE")) {
                    process_t_vote(acm);
                } else if (acm.getType().equals("AC_T_DECISION")) {
                    process_t_decision(acm);
                } else {
                    System.out.println("Unexpected message");
                }
                
                t.getMessageQueue().put(acm);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        process();
    }
}

