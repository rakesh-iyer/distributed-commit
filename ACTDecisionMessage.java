class ACTDecisionMessage extends ACMessage {
    boolean commit;

    String getType() {
        return "AC_T_DECISION";
    }

    boolean isCommited() {
        return commit;
    }

    void setCommited(boolean commit) {
        this.commit = commit;
    }
}
