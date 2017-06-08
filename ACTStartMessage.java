class ACTStartMessage extends ACMessage {
    String details;

    String getType() {
        return "AC_T_START";
    }

    String getDetails() {
        return details;
    }

    void setDetails(String details) {
        this.details = details;
    }
}
