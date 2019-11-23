package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.ServerSide;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.entity.impl.UT2ServerSide;
import one.transport.ut2.testing.entity.ClientInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

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

        Configuration.Device clientDevice = testContext.configuration.getClient();
        Configuration.Device serverDevice = testContext.configuration.getServer(serverId);
        writeClientConfiguration(clientDevice, serverDevice);
        initClientProcess();

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

        final long startTime = System.currentTimeMillis();
        sendCommand(ClientInterface.sendCommand(reqAmount, fileSize * 1024, dataFile));

        //todo add timeout
        long finishTime = 0;
        int i = 0;
        while (requestsHandler.lastRequestId != reqAmount - 1) {
            synchronized (syncObj) {
                syncObj.wait(60_000);
                finishTime = System.currentTimeMillis();
                if (i++ != requestsHandler.lastRequestId) {
                    return new TestResult("Client terminated", 0, rtt, fileSize, reqAmount,
                            lossParams, null);
                }
            }
            LOGGER.info("Progress: " + (requestsHandler.lastRequestId + 1) * 100 / reqAmount + "%");
        }

        testResult.resultTime = finishTime - startTime;
        testResult.success = true;

        sendCommand(ClientInterface.exit());
        if (!waitForClientProcess(60_000))
            return new TestResult("Client terminated", finishTime - startTime, rtt, fileSize,
                    reqAmount, lossParams, null);

        testResult.packetLoss.fromClients = testContext.tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = testContext.tunnelInterface.statistic.servers.getPacketLoss();

        deleteGeneratedFile();

        return testResult;
    }

}
