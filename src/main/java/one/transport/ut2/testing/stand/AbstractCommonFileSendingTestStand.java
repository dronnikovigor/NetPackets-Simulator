package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommonFileSendingTestStand extends AbstractTestStand {

    protected final List<AbstractClient> clientThreads = new ArrayList<>();
    protected AbstractServer serverThread;

    protected TestResult executeTest(int fileSize) throws TestErrorException {
        clientThreads.forEach(Thread::start);
        for (AbstractClient clientThread : clientThreads) {
            try {
                clientThread.join(300_000);
            } catch (InterruptedException e) {
                throw new TestErrorException("Error while join of client: " + e);
            }
        }

        TestResult testResult = setUpTestResult(fileSize);

        clientThreads.forEach(AbstractClient::clear);
        clientThreads.clear();
        return testResult;
    }

    private TestResult setUpTestResult(int fileSize) throws TestErrorException {
        TestResult testResult = new TestResult();
        testResult.fileSize = fileSize;
        testResult.rtt = tunnelInterface.rtt;
        testResult.requests = configuration.reqAmount;
        testResult.lossParams = tunnelInterface.lossParams;
        testResult.bandwidth = tunnelInterface.bandwidth;
        testResult.speedRate = tunnelInterface.speedRate;
        testResult.congestionControlWindow = tunnelInterface.getCongestionControlWindowCapacity();

        testResult.success = clientThreads.stream().allMatch(clientThread -> clientThread.getTestResult().success)
                && serverThread.getTestResult().success;
        for (AbstractClient abstractClient : clientThreads) {
            testResult.success = testResult.success && abstractClient.validateResponse();
        }
        if (!testResult.success) {
            StringBuilder error = new StringBuilder();
            for (AbstractClient abstractClient : clientThreads) {
                if (abstractClient.getTestResult().error != null)
                    error.append(abstractClient.getTestResult().error).append(" ");
                error.append(abstractClient.getError());
            }
            testResult.error = error.toString();
        } else {
            long sum = 0;
            for (AbstractClient abstractClient : clientThreads) {
                sum += abstractClient.getResultTime();
            }
            testResult.resultTime = sum / clientThreads.size();
            testResult.packetLoss.fromClients = tunnelInterface.statistic.clients.getPacketLoss();
            testResult.packetLoss.fromServers = tunnelInterface.statistic.servers.getPacketLoss();
        }
        return testResult;
    }

    protected abstract AbstractServer initServer(Configuration.Device device) throws TestErrorException;

    protected abstract AbstractClient initClient(Configuration.Device clientDevice, Configuration.Device serverDevice) throws TestErrorException;
}
