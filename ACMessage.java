class ACMessage extends Message {
    String transactionId;

    String getTransactionId() {
        return transactionId;
    }

    void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String toString() {
        return super.toString() + " transactionId " + transactionId;
    }
}
