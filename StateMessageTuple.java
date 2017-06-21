class StateMessageTuple {
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
