package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PureTcpDataTransferTestStand extends AbstractTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(PureTcpDataTransferTestStand.class);
    private final Random rnd = new Random();
    private volatile byte[] fileData;
    private ServerThread serverThread;

    @Override
    public void init(Configuration configuration, TunnelInterface tunnelInterface) {
        this.testContext = new TestContext(
                configuration,
                null,
                null,
                null,
                tunnelInterface,
                new ArrayList<>());
    }

    @Override
    public TestResult runTest(int fileSize) throws Exception {
        final TestResult result = super.runTest(fileSize);
        if (result.error != null)
            return result;

        boolean success = true;

        /* server init */
        Configuration.Device serverDevice = testContext.configuration.getServer(serverId);
        try {
            serverThread = new ServerThread(serverDevice);
            testContext.configuration.getServer(serverId).udpPort = serverThread.getPort();
            serverThread.start();
        } catch (IOException e) {
            return new TestResult("Error while binding server: " + e, 0, rtt, fileSize, reqAmount,
                    lossParams, null);
        }

        /* client info */
        Configuration.Device clientDevice = testContext.configuration.getClient();
        Socket client = new Socket();
        try {
            SocketAddress clientAddr =
                    new InetSocketAddress(InetAddress.getByAddress(clientDevice.getIpBytes()), clientDevice.tcpPort);
            client.bind(clientAddr);
            testContext.configuration.getClient().udpPort = client.getLocalPort();
        } catch (IOException e) {
            return new TestResult("Error while binding client: " + e, 0, rtt, fileSize, reqAmount,
                    lossParams, null);
        }

        try {
            byte[] serverAddr2Bytes = clientDevice.getNetAddr();
            serverAddr2Bytes[3] = serverDevice.getHostAddr();
            SocketAddress serverAddr2 =
                    new InetSocketAddress(InetAddress.getByAddress(serverAddr2Bytes), serverThread.getPort());
            client.connect(serverAddr2);
        } catch (IOException e) {
            return new TestResult("Error while connecting client: " + e, 0, rtt, fileSize,
                    reqAmount, lossParams, null);
        }

        InputStream is;
        OutputStream os;
        DataInputStream dis;
        try {
            is = client.getInputStream();
            os = client.getOutputStream();
            dis = new DataInputStream(
                    new BufferedInputStream(is)
            );
        } catch (IOException e) {
            return new TestResult("IOException while opening client output streams: " + e, 0, rtt,
                    fileSize, reqAmount, lossParams, null);
        }

        final long startTime = System.currentTimeMillis();

        fileData = new byte[fileSize * 1024];
        rnd.nextBytes(fileData);

        for (int i = 0; i < reqAmount; ++i) {
            ByteArrayOutputStream req2 = new ByteArrayOutputStream();
            req2.write(fileData, 0, 1024);

            try {
                req2.writeTo(os);
                os.flush();
                byte[] response = new byte[fileSize * 1024];
                dis.readFully(response);

                success &= Arrays.equals(response, fileData);
                LOGGER.info("Progress: " + (i + 1) * 100 / reqAmount + "%");
            } catch (IOException e) {
                return new TestResult("IOException while RW to stream: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        testResult.resultTime = System.currentTimeMillis() - startTime;
        testResult.success = success;

        testResult.packetLoss.fromClients = testContext.tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = testContext.tunnelInterface.statistic.servers.getPacketLoss();

        client.close();

        return testResult;
    }

    @Override
    public void clear() {
        if (serverThread != null) {
            serverThread.interrupt();
        }

        super.clear();
    }

    private class ServerThread extends Thread {
        final ServerSocket serverSocket;

        private ServerThread(Configuration.Device serverDevice) throws IOException {
            serverSocket = new ServerSocket();
            SocketAddress serverAddr =
                    new InetSocketAddress(InetAddress.getByAddress(serverDevice.getIpBytes()), serverDevice.tcpPort);
            serverSocket.bind(serverAddr);
        }

        @Override
        public void run() {
            final byte[] buffer = new byte[1024];
            try {
                Socket socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(is)
                );
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(os)
                );

                while (!isInterrupted()) {
                    dis.readFully(buffer);

                    dos.write(fileData);
                    dos.flush();
                }
            } catch (Exception e) {
                //ignore
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }
    }
}
