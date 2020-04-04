package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractQuicDataTransferTestStand extends AbstractCommonFileSendingTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractQuicDataTransferTestStand.class);

    protected static void createFile(String name, int size) {
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
                "X-Original-Url: http://www.example.org/testdata";

        File file = new File(name);
        byte[] data = new byte[size * 1024];
        try (FileWriter fileWriter = new FileWriter(file, false);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(String.format("%s\n\n%s", header, new String(data)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

        createFile(applicationProps.getProperty("quic.server.data.folder") + "/" + "www.example.org/testdata",
                fileSize);

        /* server init */
        Configuration.Device serverDevice = testContext.configuration.getServer(serverId);
        try {
            initServer(serverDevice);
            /* waiting server to start */
            Thread.sleep(1000);
        } catch (IOException | InterruptedException e) {
            return new TestResult("Error while binding server: " + e, 0, rtt, fileSize, reqAmount,
                    lossParams, null);
        }

        /* clients init */
        for (Configuration.Device configurationClient : testContext.configuration.getClients()) {
            final ClientThread clientThread = new ClientThread(configurationClient, serverDevice);
            if (clientThread.testResult == null)
                clientThreads.add(clientThread);
            else
                return clientThread.testResult;
        }

        return runTest();
    }

    @Override
    public void clear() {
        if (serverThread != null) {
            serverThread.clear();
        }

        super.clear();
    }

    @Override
    void initServer(Configuration.Device device) throws IOException {
        serverThread = new ServerThread(device);
        serverThread.start();
    }

    protected class ClientThread extends AbstractClientThread {
        protected ClientThread(Configuration.Device clientConf, Configuration.Device serverConf) throws UnknownHostException {
            final byte[] clientDeviceIpBytes = clientConf.getIpBytes();
            clientDeviceIpBytes[3] = serverConf.getIpBytes()[3];
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.client.binary"),
                            "--disable_certificate_verification",
                            "--allow_unknown_root_cert",
                            "--quiet",
                            "http://" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress() + ":" + serverConf.udpPort + "/testdata"
                    );

            clientErrFile = logDir.resolve("client_error_" + (clientThreads.size() + 1) + ".txt");
            clientOutFile = logDir.resolve("client_output_" + (clientThreads.size() + 1) + ".txt");
        }

        @Override
        public void run() {
            processBuilder.redirectError(clientErrFile.toFile());
            processBuilder.redirectOutput(clientOutFile.toFile());
            for (int i = 0; i < reqAmount; ++i) {
                try {
                    process = processBuilder.start();
                } catch (Exception e) {
                    testResult = new TestResult("Error while starting client: " + e, 0, rtt, fileSize,
                            reqAmount, lossParams, null);
                    return;
                }

                try {
                    process.waitFor(900_000, TimeUnit.MILLISECONDS);
                    LOGGER.info("id: " + (id + 1) + "; Progress: " + (i + 1) * 100 / reqAmount + "%");
                } catch (InterruptedException e) {
                    testResult = new TestResult("Client was terminated by timeout: " + e, 0, rtt, fileSize,
                            reqAmount, lossParams, null);
                }
            }
        }

        @Override
        void clear() {
            if (process != null && process.isAlive())
                process.destroy();
        }
    }

    protected class ServerThread extends AbstractServerThread {
        protected ProcessBuilder processBuilder;

        protected ServerThread(Configuration.Device device) throws UnknownHostException {
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.server.binary"),
                            "--quic_response_cache_dir=" + applicationProps.getProperty("quic.server.data.folder"),
                            "--certificate_file=" + applicationProps.getProperty("quic.certs.file"),
                            "--key_file=" + applicationProps.getProperty("quic.key.file"),
                            "--host=" + InetAddress.getByAddress(device.getIpBytes()).getHostAddress(),
                            "--port=" + device.udpPort
                    );
        }

        @Override
        public void run() {
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                testResult = new TestResult("Error while starting server: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        @Override
        public void clear() {
            if (process != null && process.isAlive())
                process.destroy();
        }
    }
}
