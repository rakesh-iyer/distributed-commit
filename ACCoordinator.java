import java.util.*;
import java.util.concurrent.*;

class ACCoordinator implements Runnable {
    Map<String, ACCoordinatorTransaction> transactionMap = new HashMap<>();
    Map <String, Map <Integer, Boolean>> transactionVoteMap = new HashMap<>();

    List<String> commitedTransactions = new ArrayList<>();
    List<String> abortedTransactions = new ArrayList<>();
    boolean stopThread;

    BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    final MessageReceiver messageReceiver;
    final int port;
    boolean doRecovery;
    int memberPorts[];

    ACCoordinator(int port, int[] memberPorts, boolean doRecovery) {
        this.port = port;
        this.memberPorts = memberPorts;
        this.doRecovery = doRecovery;

        messageReceiver = new MessageReceiver(messageQueue, port);
        new Thread(messageReceiver).start();
    }

    int getPort() {
        return port;
    }

    int[] getMemberPorts() {
        return memberPorts;
    }

    void sendToMembers(Message m) {
        for (int port : memberPorts) {
            System.out.println(port);
            MessageSender.send(m, port);
        }
    }

    void process() {
        try {
            while (!stopThread) {
                ACMessage acm = (ACMessage)messageQueue.take();
                String tid = acm.getTransactionId();

                System.out.println("Got a message " + acm);

                if (acm.getType().equals("AC_T_START")) {
                    ACCoordinatorTransaction t =  new ACCoordinatorTransaction(this, tid);
                    transactionMap.put(tid, t);
                    new Thread(t).start();
                }

                ACCoordinatorTransaction t = transactionMap.get(tid);
                t.getMessageQueue().put(acm);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (doRecovery) {
            recover();
        }

        process();
    }

    public void recover() {
    }
}
