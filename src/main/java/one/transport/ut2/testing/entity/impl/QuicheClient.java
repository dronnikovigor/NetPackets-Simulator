package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractQuicClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;
import one.transport.ut2.testing.utils.ProcessOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicheClient extends AbstractQuicClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuicheClient.class);

    private static final String OUTPUT_NAME = "%dKB_client_%d_output.txt";

    private String[] cmd;
    private Path outputFile;


    public QuicheClient(Configuration.Device clientConf, Configuration.Device serverConf, int id, Path logDir) throws TestErrorException {
        super(id, logDir);
        try {
            final byte[] clientDeviceIpBytes = clientConf.getIpBytes();
            clientDeviceIpBytes[3] = serverConf.getIpBytes()[3];
            cmd = new String[]{"/bin/sh", "-c",
                    "cd " + applicationProps.getProperty("quiche.home.folder") + ";" +
                            " RUST_LOG=info " + applicationProps.getProperty("quiche.client.binary") +
                            " --no-verify" +
                            " --dump-responses \"" + logDir.toAbsolutePath() + "\"" +
                            " -n " + reqAmount +
                            " http://" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress() + ":" + serverConf.udpPort + "/" + fileSize + "KB"
            };
            outputFile = logDir.resolve(String.format(OUTPUT_NAME, fileSize, id + 1));
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }

    @Override
    public void run() {
        try {
            Files.createFile(outputFile);
            Process process = Runtime.getRuntime().exec(cmd);
            ProcessOutputStream outputGobbler = new ProcessOutputStream(process.getErrorStream(), outputFile);
            outputGobbler.start();

            try (FileInputStream fileInputStream = new FileInputStream(outputFile.toFile());
                 InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                final long startTime = System.currentTimeMillis();
                while (true) {
                    if (System.currentTimeMillis() - startTime <= 100_000) {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (line.contains("connection closed")) {
                                testResult.success = true;
                                LOGGER.info("FileSize: " + fileSize + "KB; Client id: " + (id + 1) + "; Progress: 100%");
                                break;
                            }
                        }
                    } else {
                        testResult = new TestResult("Client timeout exception");
                        break;
                    }
                }
            } catch (IOException e) {
                testResult = new TestResult("Error while reading client output: " + e);
            }
            outputGobbler.interrupt();
        } catch (IOException e) {
            testResult = new TestResult("Error while starting client: " + e);
        }
    }

    @Override
    public boolean validateResponse() {
        boolean result = false;
        try (FileInputStream fileInputStream = new FileInputStream(outputFile.toFile());
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.contains("response(s) received")) {
                    String[] strings = line.split(" ");
                    for (String string : strings) {
                        if (string.contains("/")) {
                            String[] split = string.split("/");
                            if (Integer.parseInt(split[0]) == Integer.parseInt(split[1]) && Integer.parseInt(split[0]) == reqAmount)
                                result = true;
                            break;
                        }
                    }
                    break;
                }
        } catch (IOException e) {
            testResult = new TestResult("Error while reading client output: " + e);
        }

        return result;
    }

    @Override
    public String getError() throws TestErrorException {
        StringBuilder result = new StringBuilder();
        result.append("Client id: ").append(id + 1).append("\n");
        try (FileInputStream fileInputStream = new FileInputStream(outputFile.toFile());
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.contains("ERROR")) {
                    result.append(line).append("\n");
                }
        } catch (IOException e) {
            throw new TestErrorException("Error while reading client output: " + e);
        }

        return result.toString();
    }

    @Override
    public long getResultTime() {
        try (FileInputStream fileInputStream = new FileInputStream(outputFile.toFile());
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.contains("response(s) received")) {
                    String[] lineByWord = line.split(" ");
                    for (String word : lineByWord) {
                        if (word.contains("ms,")) {
                            String number = word.replaceAll("ms,", "");
                            return (long) Double.parseDouble(number);
                        }
                        if (word.contains("s,")) {
                            String number = word.replaceAll("s,", "");
                            return (long) (Double.parseDouble(number) * 1000);
                        }
                    }
                }
        } catch (IOException | NumberFormatException e) {
            testResult = new TestResult("Error while reading client output: " + e);
        }

        return 0;
    }
}
