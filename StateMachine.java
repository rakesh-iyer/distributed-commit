import java.util.concurrent.*;

abstract class StateMachine {
    State currentState;
    BlockingQueue<Message> messageQueue;

    void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    State getCurrentState() {
        return currentState;
    }

    void setMessageQueue(BlockingQueue<Message> messageQueue) {
        this.messageQueue = messageQueue;
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    abstract void sendMessage(Message m, int port);
    abstract void sendBroadcast(Message m);
    abstract void writeLogRecord(LogRecord r);

    void execute() throws InterruptedException {
        while (currentState != null) {
            Message m = null;
            try {
                // some of the states are simple transitions, with no expected messages.
                if (currentState.getExpectedMessageType() != null) {
                    m = TimedMessage.get_message_type(messageQueue, currentState.getExpectedMessageType(), currentState.messageDelay);
                }
            } catch (TimeoutException e) {
                currentState = currentState.getTimeoutState();
            }

            StateMessageTuple sm = currentState.process(m);
            currentState = sm.getState();

            // If present write log record before sending any responses.
            LogRecord record = sm.getLogRecord();
            if (record != null) {
                writeLogRecord(record);
            }

            // send a response or broadcast if present.
            Message response = sm.getMessage();
            if (response != null) {
                if (sm.getMessageType() == StateMessageTuple.MessageType.RESPONSE) {
                    sendMessage(response, m.getSenderPort());
                } else {
                    sendBroadcast(response);
                }
            }
        }
    }

    void recover() {
    }
}
