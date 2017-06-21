import java.util.concurrent.*;

abstract class StateMachine {
    State currentState;
    BlockingQueue<Message> messageQueue;
    long messageDelay;

    void setMessageQueue(BlockingQueue<Message> messageQueue) {
        this.messageQueue = messageQueue;
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }
    void setMessageDelay(long messageDelay) {
        this.messageDelay = messageDelay;
    }

    long getMessageDelay() {
        return messageDelay;
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
                    m = TimedMessage.get_message_type(messageQueue, currentState.getExpectedMessageType(), messageDelay);
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
            if (m != null) {
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
