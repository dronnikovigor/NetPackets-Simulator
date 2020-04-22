package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.AbstractClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractQuicDataTransferTestStand extends AbstractCommonFileSendingTestStand {

    @Override
    public void init(Configuration configuration, TunnelInterface tunnelInterface) throws TestErrorException {
        super.init(configuration, tunnelInterface);

        //TODO fix overwriting one file with diff IP
        Set<String> serverAddresses = new HashSet<>();
        for (Configuration.Device configurationServer : configuration.getServers()) {
            for (Configuration.Device configurationClient : configuration.getClients()) {
                final byte[] clientDeviceIpBytes = configurationClient.getIpBytes();
                clientDeviceIpBytes[3] = configurationServer.getIpBytes()[3];
                try {
                    String hostAddress = InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress();
                    serverAddresses.add(hostAddress + ":" + configurationServer.udpPort);
                } catch (UnknownHostException e) {
                    throw new TestErrorException("Unknown Host: " + e);
                }
            }
        }
        for (String serverAddress : serverAddresses) {
            generateTestFiles(serverAddress);
        }
    }

    @Override
    public TestResult runTest(int fileSize) throws TestErrorException {
        super.runTest(fileSize);

        /* server init */
        Configuration.Device serverDevice = configuration.getServer(serverId);
        try {
            serverThread = initServer(serverDevice);
            serverThread.start();
            /* waiting server to start */
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new TestErrorException("Error while binding server: " + e);
        }

        /* clients init */
        for (Configuration.Device configurationClient : configuration.getClients()) {
            final AbstractClient clientThread = initClient(configurationClient, serverDevice);
            clientThreads.add(clientThread);
        }

        return runTest();
    }

    @Override
    public void clear() throws TestErrorException {
        if (serverThread != null) {
            serverThread.clear();
        }

        super.clear();
    }

    @Override
    protected void createFile(String path, int size, String serverAddress) throws TestErrorException {
        String header = "HTTP/1.1 200 OK\n" +
                "Accept-Ranges: bytes\n" +
                "Cache-Control: max-age=604800\n" +
                "Date: Fri, 22 Nov 2019 11:20:59 GMT\n" +
                "Etag: \"3147526947\"\n" +
                "Expires: Fri, 06 Dec 2019 11:20:59 GMT\n" +
                "Last-Modified: Thu, 17 Oct 2019 07:18:26 GMT\n" +
                "Server: ECS (nyb/1D1E)\n" +
                "Vary: Accept-Encoding\n" +
                "X-Cache: HIT\n" +
                "Content-Length: " + size * 1024 + "\n" +
                "X-Original-Url: http://" + serverAddress + "/" + size + "KB";

        File file = new File(path);
        byte[] data = new byte[size * 1024];
        try (FileWriter fileWriter = new FileWriter(file, false);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(String.format("%s\n\n%s", header, new String(data)));
        } catch (IOException e) {
            throw new TestErrorException("Error while generating files: " + e);
        }
    }
}
