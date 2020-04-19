package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.*;
import one.transport.ut2.testing.entity.impl.PureTcpClient;
import one.transport.ut2.testing.entity.impl.PureTcpServer;
import one.transport.ut2.testing.stand.AbstractCommonFileSendingTestStand;

import java.util.Random;

public class PureTcpDataTransferTestStand extends AbstractCommonFileSendingTestStand {
    private final Random rnd = new Random();

    @Override
    public TestResult runTest(int fileSize) throws TestErrorException {
        super.runTest(fileSize);

        byte[] fileData = new byte[fileSize * 1024];
        rnd.nextBytes(fileData);

        /* server init */
        Configuration.Device serverDevice = configuration.getServer(serverId);
        serverThread = initServer(serverDevice);
        serverThread.setFileData(fileData);
        serverThread.start();

        /* clients init */
        final Configuration.Device[] configurationClients = configuration.getClients();
        for (Configuration.Device configurationClient : configurationClients) {
            final AbstractClient clientThread = initClient(configurationClient, serverDevice);
            clientThread.setFileData(fileData);
            clientThreads.add(clientThread);
        }

        return runTest();
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
        return new PureTcpClient(clientDevice, serverDevice, clientThreads.size(), logDir, fileSize);
    }


}
