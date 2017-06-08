import java.net.*;
import java.util.*;
import java.io.*;

class AtomicCommit {
    static final Random r = new Random();
    Queue<Message> messageQueue = new LinkedList<>();
    final int port;
    final MessageReceiver messageReceiver;
    boolean receiverStopped;
    int packetLength = 1000;
    int peerPorts[];

    AtomicCommit(int port, int peerPorts[]) {
        this.port = port;
        this.peerPorts = peerPorts;
        messageReceiver = new MessageReceiver(messageQueue, port);
    }

    class ACParticipantThread implements Runnable {
        Map <String, Boolean> transactionStatusMap = new HashMap<>();
        boolean stopThread;
        Random r = new Random();

        void process() {
            while (!stopThread) {
                Message m = messageQueue.remove();

                if (m.getType().equals("AC_T_START")) {
                    ACTStartMessage acm = (ACTStartMessage)m;
                    String tid = acm.getTransactionId();

                    // Just get a pseudo-random commit/abort status.
                    boolean commit = getRandomCommitStatus();

                    if (transactionStatusMap.get(tid) != null) {
                        System.out.println("duplicate message for tid - " + tid);
                        continue;
                    }

                    transactionStatusMap.put(tid, commit);
                } else if (m.getType().equals("AC_T_VOTE")) {
                    ACTVoteMessage acvm = (ACTVoteMessage)m;
                    int coordinatorPort = acvm.getSenderPort();
                    String tid = acvm.getTransactionId();

                    if (transactionStatusMap.get(tid) == null) {
                        // we got a vote message before start message, do we wait for vote to arrive?
                        continue;
                    }

                    ACTVoteResponseMessage acvrm = new ACTVoteResponseMessage();

                    acvrm.setCommited(transactionStatusMap.get(tid));
                    acvrm.setSenderPort(port);

                    MessageSender.send(acvrm, coordinatorPort);
                } else {
                    System.out.println("Whats this message about - " + m);
                }
            }
        }

        // pseudo probability of 0.1 returning false.
        boolean getRandomCommitStatus() {
            return r.nextInt(11) < 10;
        }

        public void run() {
            process();
        }
    }

    class ACCoordinatorThread implements Runnable {
        Map <String, Map<Integer, Boolean>> transactionVoteMap = new HashMap<>();
        boolean stopThread;

        void process() {
            while (!stopThread) {
                Message m = messageQueue.remove();

                if (m.getType().equals("AC_T_START")) {
                    ACTStartMessage acm = (ACTStartMessage)m;
                    String tid = acm.getTransactionId();

                    transactionVoteMap.put(tid, new HashMap<>());

                    ACTVoteMessage atvm = new ACTVoteMessage();
                    atvm.setTransactionId(tid);

                    sendToPeers(atvm);
                } else if (m.getType().equals("AC_T_VOTE_RESPONSE")) {
                    ACTVoteResponseMessage actvm = (ACTVoteResponseMessage) m;

                    String tid = actvm.getTransactionId();
                    Integer senderPort = actvm.getSenderPort();
                    Boolean commit = actvm.isCommited();

                    Map <Integer, Boolean> votes = transactionVoteMap.get(tid);

                    if (votes == null) {
                        System.out.println("Got a vote for a request never sent. " + tid);
                    }

                    votes.put(senderPort, commit);

                    // if you receive all the votes or any abort go ahead and send abort decision.
                } else {
                    System.out.println("Whats this message about - " + m);
                }
            }
        }

        public void run() {
            process();
        }
    }    

    void start() {
        try {
            messageReceiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void sendToPeers(Message m) {
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

/*    void sendTestMessagesToPeers() {
        System.out.println("Sending a message to peers");

        Thread.sleep(10000);
        Message m = new Message();
        m.setType("T_START");
        m.setData("T Details");
        m.setSenderPort(port);
        while(true) {
            sendToPeers(m);
            Thread.sleep(1000);
        }
    }
*/
    void stop() {
        try {

//            messageReceiver.halt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        int port;
        int peerPorts[];
        try {
            int peerCount = args.length - 1;

            port = Integer.valueOf(args[0]);
            peerPorts = new int[peerCount];

            for (int i = 0; i < peerCount; i++) {
                peerPorts[i] = Integer.valueOf(args[i+1]);
            }

        } catch (NumberFormatException e) {
            System.out.println("Bad port number or peer port number");
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Did not provide port number");
            return;
        }

        AtomicCommit p = new AtomicCommit(port, peerPorts);

        p.start();
        p.stop();
        // find other participants.
    }
}
