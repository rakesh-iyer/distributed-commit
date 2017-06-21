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
    }

    BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    void sendResponse(Message original, Message response) {
        response.setSenderPort(member.getPort());

        member.sendToHost(response, original.getSenderPort());
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
        execute();
    }
}
