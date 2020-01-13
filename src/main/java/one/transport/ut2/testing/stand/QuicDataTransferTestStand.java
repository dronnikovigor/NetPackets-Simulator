package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicDataTransferTestStand extends AbstractCommonFileSendingTestStand {
    private Process serverProcess;

    private static void createFile(String name, int size) {
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
            serverProcess = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.server.binary"),
                            "--quic_response_cache_dir=" + applicationProps.getProperty("quic.server.data.folder"),
                            "--certificate_file=" + applicationProps.getProperty("quic.certs.file"),
                            "--key_file=" + applicationProps.getProperty("quic.key.file"),
                            "--host=" + InetAddress.getByAddress(serverDevice.getIpBytes()).getHostAddress(),
                            "--port=" + serverDevice.udpPort
                    )
                    .start();
            /* waiting server to start */
            Thread.sleep(2000);
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
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
        }

        super.clear();
    }

    private class ClientThread extends AbstractClientThread {
        private ProcessBuilder processBuilder;
        private Process process;

        private Path clientErrFile;
        private Path clientOutFile;

        private ClientThread(Configuration.Device clientConf, Configuration.Device serverConf) throws UnknownHostException {
            final byte[] clientDeviceIpBytes = clientConf.getIpBytes();
            clientDeviceIpBytes[3] = serverConf.getIpBytes()[3];
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.client.binary"),
                            "--host=" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress(),
                            "--port=" + serverConf.udpPort,
                            "--disable_certificate_verification",
                            "--allow_unknown_root_cert",
                            "--num_requests=" + reqAmount,
                            "http://www.example.org/testdata",
                            "--v=2"
                    );

            clientErrFile = logDir.resolve("client_error_" + (clientThreads.size() + 1) + ".txt");
            clientOutFile = logDir.resolve("client_output_" + (clientThreads.size() + 1) + ".txt");

            try {
                Files.deleteIfExists(clientErrFile);
                Files.deleteIfExists(clientOutFile);
                Files.createFile(clientErrFile);
                Files.createFile(clientOutFile);
            } catch (IOException e) {
                testResult = new TestResult("Error while opening output files: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        @Override
        public void run() {
            processBuilder.redirectError(clientErrFile.toFile());
            processBuilder.redirectOutput(clientOutFile.toFile());
            try {
                process = processBuilder.start();
            } catch (Exception e) {
                testResult = new TestResult("Error while starting client: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
                return;
            }

            try {
                process.waitFor(900_000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                testResult = new TestResult("Client was terminated by timeout: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        @Override
        void clear() {
            if (process != null && process.isAlive())
                process.destroy();
        }
    }
}
