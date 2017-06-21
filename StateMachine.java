import java.util.concurrent.*;

class StateMachine {
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

    void execute() throws InterruptedException {
        while (currentState != null) {
            Message m = null;
            try {
                m = TimedMessage.get_message_type(messageQueue, currentState.getExpectedMessageType(), messageDelay);
            } catch (TimeoutException e) {
                currentState = currentState.getTimeoutState();
            }

            currentState = currentState.process(m);
        }
    }

    void recover() {
    }
}
