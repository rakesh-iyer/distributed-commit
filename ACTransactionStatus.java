class ACTransactionStatus {
    boolean commited;
    boolean started;
    boolean voting;

    boolean isCommited() {
        return commited;
    }

    boolean isStarted() {
        return started;
    }

    boolean isVoting() {
        return voting;
    }

    void setCommited(boolean commited) {
        this.commited = commited;
    }

    void setVoting(boolean voting) {
        this.voting = voting;
    }

    void setStarted(boolean started) {
        this.started = started;
    }

    public String toString() {
        return "commited " + commited + " started " + started + " voting " + voting + " coordinatorPort " + coordinatorPort;
    }
}
