abstract class State {
    State timeoutState;
    State failureState;
    String expectedMessageType;

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

    abstract State process(Message m);
}
