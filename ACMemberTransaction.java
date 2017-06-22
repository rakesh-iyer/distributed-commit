import java.util.*;
import java.util.concurrent.*;

class ACMemberTransaction extends StateMachine implements Runnable {
    ACMember member;
    BlockingQueue<Message> messageQueue;
    String tid;
    ACTransactionStatus status;
    boolean stopThread;
    long messageDelay = 100;
    Random r = new Random();

    ACMemberTransaction(ACMember member, String tid) {
        this.member = member;
        this.tid = tid;

        messageQueue = new LinkedBlockingQueue<>();
        status = new ACTransactionStatus();

        setMessageQueue(messageQueue);
        setCurrentState(new TwoPCMemberStartState());
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    void sendMessage(Message m, int port) {
        m.setSenderPort(member.getPort());

        member.sendToHost(m, port);
    }

    void sendBroadcast(Message m) {
        m.setSenderPort(member.getPort());

        member.sendToPeers(m);
    }

    void writeLogRecord(LogRecord record) {
        record.setTransactionId(tid);

        member.getLogImpl().writeRecord(record);
    }

    public void run() {
        try {
            execute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
