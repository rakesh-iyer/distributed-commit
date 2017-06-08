class ACTVoteResponseMessage extends ACMessage {
    boolean commit;

    String getType() {
        return "AC_T_VOTE_RESPONSE";
    }

    boolean isCommited() {
        return commit;
    }

    void setCommited(boolean commit) {
        this.commit = commit;
    }
}
