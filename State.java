abstract class State {
    State timeoutState;
    State failureState;
    String expectedMessageType;

    static class StateMessageTuple {
        State state;
        Message message;
        MessageType messageType;
        LogRecord record;

        enum MessageType {
            RESPONSE,
            BROADCAST
        }

        StateMessageTuple(State state, Message message, MessageType messageType, LogRecord record) {
            this.state = state;
            this.message = message;
            this.messageType = messageType;
            this.record = record;
        }

        State getState() {
            return state;
        }

        Message getMessage() {
            return message;
        }

        MessageType getMessageType() {
            return messageType;
        }

        LogRecord getLogRecord() {
            return record;
        }
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
