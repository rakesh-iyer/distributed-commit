class ACTVoteResponseMessage extends ACMessage {
    boolean commit;

    ACTVoteResponseMessage() {
        super.setType("AC_T_VOTE_RESPONSE");
    }

    boolean isCommited() {
        return commit;
    }

    void setCommited(boolean commit) {
        this.commit = commit;
    }
}
