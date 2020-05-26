package one.transport.ut2.testing.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public abstract class AbstractQuicClient extends AbstractClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractQuicClient.class);

    protected static final String errorName = "%dKB_client_%d_error_req_%d.txt";
    protected static final String outputName = "%dKB_client_%d_error_req_%d.txt";

    protected AbstractQuicClient(int id, Path logDir) {
        super(id, logDir);
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        for (int i = 0; i < reqAmount; ++i) {
            try {
                ;
                Path clientErrFile = logDir.resolve(String.format(errorName, fileSize, getClientId(), i + 1));
                Path clientOutFile = logDir.resolve(String.format(outputName, fileSize, getClientId(), i + 1));
                processBuilder.redirectError(clientErrFile.toFile());
                processBuilder.redirectOutput(clientOutFile.toFile());
                process = processBuilder.start();
            } catch (Exception e) {
                testResult = new TestResult("Error while starting client: " + e);
                return;
            }

            try {
                process.waitFor(900_000, TimeUnit.MILLISECONDS);
                LOGGER.info("FileSize: " + fileSize + "KB; Client id: " + getClientId() + "; Progress: " + (i + 1) * 100 / reqAmount + "%");
            } catch (InterruptedException e) {
                testResult = new TestResult("Client was terminated by timeout: " + e);
                return;
            }
        }
        finishTime = System.currentTimeMillis();
        testResult.success = true;
    }

    @Override
    public void clear() {
        if (process != null && process.isAlive())
            process.destroy();
    }

    @Override
    public boolean validateResponse() throws TestErrorException {
        boolean[] results = new boolean[reqAmount];

        for (int i = 0; i < reqAmount; ++i) {
            Path clientOutFile = logDir.resolve(String.format(outputName, fileSize, getClientId(), i + 1));
            File file = new File(String.valueOf(clientOutFile));
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = bufferedReader.readLine()) != null)
                    if (line.contains("200 OK") || line.contains("Request succeeded (200)."))
                        results[i] = true;
            } catch (IOException e) {
                throw new TestErrorException("Can't find or read file: " + e);
            }
        }
        return IntStream.range(0, results.length).mapToObj(idx -> results[idx]).allMatch(result -> result);
    }

    @Override
    public String getError() throws TestErrorException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < reqAmount; ++i) {
            result.append("Client id: ").append(getClientId()).append(" Request: ").append(i + 1).append("\n");
            Path clientErrFile = logDir.resolve(String.format(errorName, fileSize, getClientId(), i + 1));
            File file = new File(String.valueOf(clientErrFile));
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = bufferedReader.readLine()) != null)
                    result.append(line).append("\n");
            } catch (IOException e) {
                throw new TestErrorException("Can't find or read file: " + e);
            }
        }

        return result.toString();
    }
}
