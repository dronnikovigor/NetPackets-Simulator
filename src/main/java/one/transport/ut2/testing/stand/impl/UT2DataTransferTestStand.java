package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.*;
import one.transport.ut2.testing.entity.impl.UT2ServerSide;
import one.transport.ut2.testing.stand.AbstractFileSendingTestStand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class UT2DataTransferTestStand extends AbstractFileSendingTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(UT2DataTransferTestStand.class);

    @Override
    public TestResult runTest(int fileSize) throws TestErrorException {
        super.runTest(fileSize);

        Configuration.Device serverDevice = configuration.getServer(serverId);
        for (Configuration.Device clientDevice : configuration.getClients()) {
            writeClientConfiguration(clientDevice, serverDevice);
            initClientProcess();
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        UT2ServerSide ut2ServerSide = null;
        for (ServerSide server : serverSides) {
            if (((UT2ServerSide) server).instanceId == serverId) {
                ut2ServerSide = (UT2ServerSide) server;
                break;
            }
        }
        if (ut2ServerSide == null) {
            throw new TestErrorException("UT2Server in null");
        }

        ServerHandler requestsHandler = new ServerHandler();
        ut2ServerSide.setServerHandler(requestsHandler);

        List<Thread> responseThreads = new ArrayList<>();

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < configuration.getClients().length; i++) {
            sendCommand(i, ClientInterface.sendCommand(configuration.reqAmount, fileSize * 1024, Paths.get(applicationProps.getProperty("temp.data.folder") + "/" + fileSize + "KB")));

            Thread responseThread = new Thread(() -> {
                while (requestsHandler.lastRequestId != configuration.reqAmount - 1) {
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
        for (int i = 0; i < configuration.getClients().length; i++) {
            sendCommand(i, ClientInterface.exit());
            if (!waitForClientProcess(i, 60_000))
                throw new TestErrorException("Client terminated: " + getError());
        }

        testResult.success = true;
        testResult.resultTime = finishTime - startTime;
        testResult.packetLoss.fromClients = tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = tunnelInterface.statistic.servers.getPacketLoss();

        return testResult;
    }
}
