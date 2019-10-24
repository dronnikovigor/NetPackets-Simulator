package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.entity.TestContext.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicDataTransferTestStand extends AbstractTestStand {
    private Process serverProcess;
    private Process clientProcess;

    private static void createFile(String name, int size) {
        String header = "HTTP/1.1 200 OK\n" +
                "Accept-Ranges: bytes\n" +
                "Cache-Control: max-age=604800\n" +
                "Date: Fri, 01 Nov 2019 11:20:59 GMT\n" +
                "Etag: \"3147526947\"\n" +
                "Expires: Fri, 08 Nov 2019 11:20:59 GMT\n" +
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
            return new TestResult("Error while binding server: " + e, 0, bandwidth, fileSize, reqAmount,
                    lossParams, null);
        }

        /* client info */
        Configuration.Device clientDevice = testContext.configuration.getClient();

        final byte[] clientDeviceIpBytes = clientDevice.getIpBytes();
        clientDeviceIpBytes[3] = serverDevice.getIpBytes()[3];
        ProcessBuilder clientProcessBuilder = new ProcessBuilder()
                .directory(logDir.toFile())
                .command(
                        applicationProps.getProperty("quic.client.binary"),
                        "--host=" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress(),
                        "--port=" + serverDevice.udpPort,
                        "--disable_certificate_verification",
                        "--allow_unknown_root_cert",
                        "--num_requests=" + reqAmount,
                        "http://www.example.org/testdata",
                        "--v=2"
                );

        TestResult requestTestResult = runClientRequest(clientProcessBuilder);

        testResult.success = requestTestResult.success;
        testResult.error = requestTestResult.error;
        testResult.resultTime = requestTestResult.resultTime;

        testResult.packetLoss.fromClients = testContext.tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = testContext.tunnelInterface.statistic.servers.getPacketLoss();

        return testResult;
    }

    private TestResult runClientRequest(ProcessBuilder clientProcessBuilder) {
        final TestResult requestTestResult = new TestResult();

        Path clientErrFile = logDir.resolve("client_error.txt");
        Path clientOutFile = logDir.resolve("client_output.txt");
        try {
            Files.deleteIfExists(clientErrFile);
            Files.deleteIfExists(clientOutFile);
            Files.createFile(clientErrFile);
            Files.createFile(clientOutFile);
        } catch (IOException e) {
            return new TestResult("Error while opening output files: " + e, 0, bandwidth, fileSize,
                    reqAmount, lossParams, null);
        }

        final long startTime = System.currentTimeMillis();
        final long finishTime;
        try {
            clientProcessBuilder.redirectError(clientErrFile.toFile());
            clientProcessBuilder.redirectOutput(clientOutFile.toFile());
            clientProcess = clientProcessBuilder.start();
        } catch (IOException e) {
            return new TestResult("Error while starting client: " + e, 0, bandwidth, fileSize,
                    reqAmount, lossParams, null);
        }

        double startTimeLogs = 0;
        double finishTimeLogs = 0;

        try {
            boolean close = clientProcess.waitFor(900_000, TimeUnit.MILLISECONDS);

            finishTime = System.currentTimeMillis();
            if (close) {
                /* checking for response 200 */
                try (BufferedReader reader = new BufferedReader(new FileReader(clientOutFile.toFile()))) {
                    String currentLine = reader.readLine();
                    int counter = 0;
                    while (currentLine != null) {
                        if (currentLine.contains("Request succeeded (200).")) {
                            counter++;
                        }
                        currentLine = reader.readLine();
                    }
                    if (counter == reqAmount)
                        requestTestResult.success = true;
                } catch (IOException e) {
                    return new TestResult("Error while checking response: " + e, 0, bandwidth, fileSize,
                            reqAmount, lossParams, null);
                }

                /* checking for requests time */
                try (BufferedReader reader = new BufferedReader(new FileReader(clientErrFile.toFile()))) {
                    String currentLine = reader.readLine();
                    while (currentLine != null) {
                        if (currentLine.contains("Client: Sending CHLO<") || currentLine.contains("Client: packet(1)")) {
                            currentLine = currentLine.substring(currentLine.indexOf("/") + 1);
                            startTimeLogs = Double.parseDouble(currentLine.substring(0, currentLine.indexOf(":")));
                            break;
                        }
                        currentLine = reader.readLine();
                    }
                    currentLine = reader.readLine();
                    while (currentLine != null) {
                        if (currentLine.contains("Client: Closing connection")) {
                            currentLine = currentLine.substring(currentLine.indexOf("/") + 1);
                            finishTimeLogs = Double.parseDouble(currentLine.substring(0, currentLine.indexOf(":")));
                        }
                        currentLine = reader.readLine();
                    }
                } catch (IOException e) {
                    return new TestResult("Error while checking time in logs: " + e,
                            finishTime - startTime, bandwidth, fileSize, reqAmount, lossParams, null);
                }
            } else return new TestResult("Client terminated!", 0, bandwidth, fileSize, reqAmount,
                    lossParams, null);
        } catch (InterruptedException e) {
            clientProcess.destroy();
            return new TestResult("Error while running client: " + e, 0, bandwidth, fileSize, reqAmount,
                    lossParams, null);
        }

        requestTestResult.resultTime = (int) (finishTimeLogs * 1000 - startTimeLogs * 1000);
        return requestTestResult;
    }

    @Override
    public void clear() {
        if (clientProcess != null && clientProcess.isAlive()) {
            clientProcess.destroy();
        }

        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
        }

        super.clear();
    }
}
