import java.util.concurrent.*;

class ACState {
    ACState nextState;
    ACState timeoutState;
    ACState failureState;
    String messageType;
    BlockingQueue<Message> messageQueue;
    long messageDelay;

    void setNextState(ACState nextState) {
        this.nextState = nextState;
    }

    ACState getNextState() {
        return nextState;
    }

    void setTimeoutState(ACState timeoutState) {
        this.timeoutState = timeoutState;
    }

    ACState getTimeoutState() {
        return timeoutState;
    }

    void setFailureState(ACState failureState) {
        this.failureState = failureState;
    }

    ACState getFailureState() {
        return failureState;
    }

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

    void executeState(Message m) throws InterruptedException {
        try {
            Message transitionMessage = TimedMessage.get_message_type(messageQueue, messageType, messageDelay);
            nextState.executeState(transitionMessage);
        } catch (TimeoutException e) {
            timeoutState.executeState(null);
        }
    }
}
