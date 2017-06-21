import java.util.*;
import java.util.concurrent.*;

class ACCoordinatorTransaction extends StateMachine implements Runnable {
    ACCoordinator coordinator;
    BlockingQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;
    Map<Integer, Boolean> voteMap = new HashMap<>();
    long messageDelay = 100;
    boolean stopThread;

    ACCoordinatorTransaction(ACCoordinator coordinator, String tid) {
        this.coordinator = coordinator;
        this.tid = tid;

        messageQueue = new LinkedBlockingQueue<>();
        status = new ACTransactionStatus();

        setCurrentState(new TwoPCCoordinatorStartState(coordinator.getMemberPorts()));
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    void sendMessage(Message m, int port) {
        m.setSenderPort(coordinator.getPort());

        coordinator.sendToHost(m, port);
    }

    void sendBroadcast(Message m) {
        m.setSenderPort(coordinator.getPort());

        coordinator.sendToMembers(m);
    }

    void writeLogRecord(LogRecord record) {
        record.setTransactionId(tid);

        coordinator.getLogImpl().writeRecord(record);
    }

    public void run() {
        try {
            execute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
