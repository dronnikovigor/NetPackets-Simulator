package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommonFileSendingTestStand extends AbstractTestStand {

    protected final List<AbstractClient> clientThreads = new ArrayList<>();
    protected AbstractServer serverThread;

    protected TestResult runTest() throws TestErrorException {
        final long startTime = System.currentTimeMillis();
        AbstractClient.setReqAmount(configuration.reqAmount);
        clientThreads.forEach(Thread::start);
        for (AbstractClient clientThread : clientThreads) {
            try {
                clientThread.join(300_000);
            } catch (InterruptedException e) {
                throw new TestErrorException("Error while join of client: " + e);
            }
        }
        final long finishTime = System.currentTimeMillis();

        setUpTestResult(finishTime - startTime);

        return testResult;
    }

    private void setUpTestResult(long resultTime) throws TestErrorException {
        testResult.success = clientThreads.stream().allMatch(clientThread -> clientThread.getTestResult().success)
                && serverThread.getTestResult().success;
        for (AbstractClient abstractClient : clientThreads) {
            testResult.success = testResult.success && abstractClient.validateResponse();
        }
        if (!testResult.success) {
            StringBuilder error = new StringBuilder();
            clientThreads.forEach(clientThread -> {
                if (clientThread.getTestResult().error != null)
                    error.append(clientThread.getTestResult().error).append(" ");
            });
            testResult.error = error.toString();
        } else {
            testResult.resultTime = resultTime;
            testResult.packetLoss.fromClients = tunnelInterface.statistic.clients.getPacketLoss();
            testResult.packetLoss.fromServers = tunnelInterface.statistic.servers.getPacketLoss();
        }
    }

    public void clear() throws TestErrorException {
        clientThreads.forEach(AbstractClient::clear);
        clientThreads.clear();

        super.clear();
    }

    protected abstract AbstractServer initServer(Configuration.Device device) throws TestErrorException;

    protected abstract AbstractClient initClient(Configuration.Device clientDevice, Configuration.Device serverDevice) throws TestErrorException;
}
