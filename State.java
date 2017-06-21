abstract class State {
    State timeoutState;
    State failureState;
    String expectedMessageType;
    long messageDelay;

    void setMessageDelay(long messageDelay) {
        this.messageDelay = messageDelay;
    }

    long getMessageDelay() {
        return messageDelay;
    }

    void setExpectedMessageType(String expectedMessageType) {
        this.expectedMessageType = expectedMessageType;
    }

    String getExpectedMessageType() {
        return expectedMessageType;
    }

    void setTimeoutState(State timeoutState) {
        this.timeoutState = timeoutState;
    }

    State getTimeoutState() {
        return timeoutState;
    }

    void setFailureState(State failureState) {
        this.failureState = failureState;
    }

    State getFailureState() {
        return failureState;
    }

    abstract StateMessageTuple process(Message m);
}
