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

public class PureTcpDataTransferTestStand extends AbstractCommonFileSendingTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(PureTcpDataTransferTestStand.class);
    private final Random rnd = new Random();
    private byte[] fileData;
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

        fileData = new byte[fileSize * 1024];
        rnd.nextBytes(fileData);

        /* server init */
        Configuration.Device serverDevice = testContext.configuration.getServer(serverId);
        try {
            initServer(serverDevice);
        } catch (IOException e) {
            return new TestResult("Error while binding server: " + e, 0, rtt, fileSize, reqAmount,
                    lossParams, null);
        }

        /* clients init */
        final Configuration.Device[] configurationClients = testContext.configuration.getClients();
        for (Configuration.Device configurationClient : configurationClients) {
            final ClientThread clientThread = new ClientThread(configurationClient, serverDevice);
            /* checking for init errors */
            if (clientThread.testResult == null)
                clientThreads.add(clientThread);
            else
                return clientThread.testResult;
        }

        return runTest();
    }

    @Override
    public void clear() {
        super.clear();

        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Override
    void initServer(Configuration.Device device) throws IOException {
        serverThread = new ServerThread(device);
        device.tcpPort = serverThread.getPort();
        serverThread.start();
    }

    private class ClientThread extends AbstractClientThread {
        final private Socket socket;

        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;

        private TestResult testResult;

        ClientThread(Configuration.Device clientConf, Configuration.Device serverConf) {
            socket = new Socket();
            try {
                SocketAddress clientAddr =
                        new InetSocketAddress(InetAddress.getByAddress(clientConf.getIpBytes()), 0);
                socket.bind(clientAddr);
                clientConf.tcpPort = socket.getLocalPort();
            } catch (IOException e) {
                testResult = new TestResult("Error while binding client: " + e, 0, rtt, fileSize, reqAmount,
                        lossParams, null);
                return;
            }
            try {
                byte[] serverAddr2Bytes = clientConf.getNetAddr();
                serverAddr2Bytes[3] = serverConf.getHostAddr();
                SocketAddress serverAddr =
                        new InetSocketAddress(InetAddress.getByAddress(serverAddr2Bytes), serverThread.getPort());
                socket.connect(serverAddr);
            } catch (IOException e) {
                testResult = new TestResult("Error while connecting client: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
                return;
            }
            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();
                dis = new DataInputStream(
                        new BufferedInputStream(is)
                );
            } catch (IOException e) {
                testResult = new TestResult("IOException while opening client output streams: " + e, 0, rtt,
                        fileSize, reqAmount, lossParams, null);
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < reqAmount; ++i) {
                ByteArrayOutputStream req2 = new ByteArrayOutputStream();
                req2.write(fileData, 0, 1024);

                try {
                    req2.writeTo(os);
                    os.flush();
                    byte[] response = new byte[fileSize * 1024];
                    dis.readFully(response);

                    final boolean success = Arrays.equals(response, fileData);
                    if (!success)
                        testResult = new TestResult("Data was corrupted!", 0, rtt,
                                fileSize, reqAmount, lossParams, null);
                    LOGGER.info("Progress: " + (i + 1) * 100 / reqAmount + "%");
                } catch (IOException e) {
                    testResult = new TestResult("IOException while RW to stream: " + e, 0, rtt,
                            fileSize, reqAmount, lossParams, null);
                }
            }
        }

        @Override
        void clear() {
        }
    }

    private class ServerThread extends Thread {
        final private ServerSocket serverSocket;

        private ServerThread(Configuration.Device serverDevice) throws IOException {
            serverSocket = new ServerSocket();
            SocketAddress serverAddr =
                    new InetSocketAddress(InetAddress.getByAddress(serverDevice.getIpBytes()), 0);
            serverSocket.bind(serverAddr);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        LOGGER.info("New connection opened: " + socket.getRemoteSocketAddress());
                        final byte[] buffer = new byte[1024];
                        try {
                            InputStream is = socket.getInputStream();
                            OutputStream os = socket.getOutputStream();
                            DataInputStream dis = new DataInputStream(
                                    new BufferedInputStream(is)
                            );
                            DataOutputStream dos = new DataOutputStream(
                                    new BufferedOutputStream(os)
                            );

                            while (!ServerThread.this.isInterrupted() && socket.isConnected()) {
                                try {
                                    dis.readFully(buffer);

                                    dos.write(fileData);
                                    dos.flush();
                                } catch (EOFException e) {
                                    //TODO fix exception
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }
    }
}
