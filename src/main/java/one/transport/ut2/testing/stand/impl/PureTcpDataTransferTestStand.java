package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.*;
import one.transport.ut2.testing.entity.impl.PureTcpClient;
import one.transport.ut2.testing.entity.impl.PureTcpServer;
import one.transport.ut2.testing.stand.AbstractCommonFileSendingTestStand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PureTcpDataTransferTestStand extends AbstractCommonFileSendingTestStand {
    private final Random rnd = new Random();

    @Override
    public List<TestResult> runTest() throws TestErrorException {
        List<TestResult> testResults = super.runTest();

        Map<Integer, byte[]> filesData = new HashMap<>();
        for (int fileSize : configuration.fileSizes) {
            byte[] fileData = new byte[fileSize * 1024];
            rnd.nextBytes(fileData);
            filesData.put(fileSize, fileData);
        }

        /* server init */
        Configuration.Device serverDevice = configuration.getServer(serverId);
        serverThread = initServer(serverDevice);
        serverThread.setFilesData(filesData);
        serverThread.start();

        for (int fileSize: configuration.fileSizes) {
            /* clients init */
            AbstractClient.setFileSize(fileSize);
            final Configuration.Device[] configurationClients = configuration.getClients();
            for (Configuration.Device configurationClient : configurationClients) {
                final AbstractClient clientThread = initClient(configurationClient, serverDevice);
                clientThread.setFilesData(filesData);
                clientThreads.add(clientThread);
            }
            testResults.add(executeTest(fileSize));
        }

        return testResults;
    }

    @Override
    public void clear() throws TestErrorException {
        super.clear();

        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Override
    protected void createFile(String path, int size, String serverAddress) {

    }

    @Override
    protected AbstractServer initServer(Configuration.Device device) throws TestErrorException {
        return new PureTcpServer(device, logDir);
    }

    @Override
    protected AbstractClient initClient(Configuration.Device clientDevice, Configuration.Device serverDevice) throws TestErrorException {
        return new PureTcpClient(clientDevice, serverDevice, clientThreads.size(), logDir);
    }
}
