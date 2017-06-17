import java.util.*;
import java.util.concurrent.*;

class TimedMessage {
    static Message get_message_type(BlockingQueue<Message> messageQueue, String messageType, long timeout) throws TimeoutException, InterruptedException {
        Message m;
        long startMillis = System.currentTimeMillis();

        while (true) {
            long sleepTime = timeout - (System.currentTimeMillis() - startMillis);

            m = messageQueue.poll(sleepTime, TimeUnit.MILLISECONDS);

            if (m == null) {
                throw new TimeoutException("Timed out for message of type " + messageType);
            } else if (m.getType().equals(messageType)) {
                break;
            }
        }

        return m;
    }
}

