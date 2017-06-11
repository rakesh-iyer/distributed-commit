class ACTDecisionMessage extends ACMessage {
    boolean commit;

    ACTDecisionMessage() {
        super.setType("AC_T_DECISION");
    }

    boolean isCommited() {
        return commit;
    }

    void setCommited(boolean commit) {
        this.commit = commit;
    }

    public String toString() {
        return super.toString() + " commit " + commit;
    }
}
