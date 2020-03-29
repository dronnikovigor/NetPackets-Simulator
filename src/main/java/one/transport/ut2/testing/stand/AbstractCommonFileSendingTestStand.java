package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractCommonFileSendingTestStand extends AbstractTestStand {

    List<AbstractClientThread> clientThreads = new ArrayList<>();
    AbstractServerThread serverThread;

    protected TestContext.TestResult runTest() {
        final long startTime = System.currentTimeMillis();
        clientThreads.forEach(Thread::start);
        clientThreads.forEach(clientThread -> {
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        final long finishTime = System.currentTimeMillis();

        testResult.resultTime = finishTime - startTime;
        testResult.success = clientThreads.stream().allMatch(clientThread -> clientThread.testResult == null);
        if (!testResult.success) {
            StringBuilder error = new StringBuilder();
            clientThreads.forEach(clientThread -> {
                if (clientThread.testResult.error != null)
                    error.append(clientThread.testResult.error).append(" ");
            });
            testResult.error = error.toString();
        } else {
            testResult.packetLoss.fromClients = testContext.tunnelInterface.statistic.clients.getPacketLoss();
            testResult.packetLoss.fromServers = testContext.tunnelInterface.statistic.servers.getPacketLoss();
        }
        return testResult;
    }

    public void clear() {
        clientThreads.forEach(AbstractClientThread::clear);
        clientThreads.clear();

        super.clear();
    }

    abstract void initServer(Configuration.Device device) throws IOException;

    abstract class AbstractClientThread extends Thread {
        protected int id;
        protected ProcessBuilder processBuilder;
        protected Process process;

        protected Path clientErrFile;
        protected Path clientOutFile;

        TestContext.TestResult testResult;

        abstract void clear();

        AbstractClientThread() {
            id = clientThreads.size();
        }
    }

    abstract class AbstractServerThread extends Thread {
        protected Process process;

        abstract void clear();
    }
}
