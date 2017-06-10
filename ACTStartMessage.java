class ACTStartMessage extends ACMessage {
    String details;

    ACTStartMessage() {
        super.setType("AC_T_START");
    }

    String getDetails() {
        return details;
    }

    void setDetails(String details) {
        this.details = details;
    }
}
