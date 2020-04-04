package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.ClientInterface;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.ServerSide;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.entity.impl.UT2ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class UT2DataTransferTestStand extends AbstractFileSendingTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(UT2DataTransferTestStand.class);

    @Override
    public TestResult runTest(int fileSize) throws Exception {
        final TestResult result = super.runTest(fileSize);
        if (result.error != null)
            return result;

        initLogDir(applicationProps.getProperty("log.dir"),
                fileSize + "KB_" + rtt + "ms__PL_" + lossParams.getName());

        Configuration.Device serverDevice = testContext.configuration.getServer(serverId);
        for (Configuration.Device clientDevice : testContext.configuration.getClients()) {
            writeClientConfiguration(clientDevice, serverDevice);
            initClientProcess();
            //Thread.sleep(1000);
        }

        UT2ServerSide ut2ServerSide = null;
        for (ServerSide server : testContext.serverSides) {
            if (((UT2ServerSide) server).instanceId == serverId) {
                ut2ServerSide = (UT2ServerSide) server;
                break;
            }
        }
        if (ut2ServerSide == null) {
            return new TestResult("UT2Server in null", 0, rtt, fileSize, reqAmount,
                    lossParams, null);
        }

        ServerHandler requestsHandler = new ServerHandler();
        ut2ServerSide.setServerHandler(requestsHandler);

        Path dataFile = generateFileBySize(logDir, fileSize);

        List<Thread> responseThreads = new ArrayList<>();

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < testContext.configuration.getClients().length; i++) {
            sendCommand(i, ClientInterface.sendCommand(reqAmount, fileSize * 1024, dataFile));

            Thread responseThread = new Thread(() -> {
                while (requestsHandler.lastRequestId != reqAmount - 1) {
                    synchronized (syncObj) {
                        try {
                            syncObj.wait(60_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            responseThread.start();
            responseThreads.add(responseThread);
        }

        for (int i = 0; i < responseThreads.size(); i++) {
            Thread responseThread = responseThreads.get(i);
            try {
                responseThread.join(60_000);
                LOGGER.info("Progress: " + (i + 1) * 100 / responseThreads.size() + "%");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        final long finishTime = System.currentTimeMillis();

        //if test isn't success, this will return false
        for (int i = 0; i < testContext.configuration.getClients().length; i++) {
            sendCommand(i, ClientInterface.exit());
            if (!waitForClientProcess(i, 60_000))
                return new TestResult("Client terminated: " + getError(), 0, rtt, fileSize,
                        reqAmount, lossParams, null);
        }

        testResult.success = true;
        testResult.resultTime = finishTime - startTime;
        testResult.packetLoss.fromClients = testContext.tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = testContext.tunnelInterface.statistic.servers.getPacketLoss();

        deleteGeneratedFile();

        return testResult;
    }
}
