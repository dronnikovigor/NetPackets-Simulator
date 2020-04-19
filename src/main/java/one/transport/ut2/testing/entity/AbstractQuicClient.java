package one.transport.ut2.testing.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public abstract class AbstractQuicClient extends AbstractClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractQuicClient.class);

    protected AbstractQuicClient(int id, Path logDir, int fileSize) {
        super(id, logDir, fileSize);
    }

    @Override
    public void run() {
        for (int i = 0; i < reqAmount; ++i) {
            try {
                Path clientErrFile = logDir.resolve("client_error_" + id + "__" + i + ".txt");
                Path clientOutFile = logDir.resolve("client_output_" + id + "__" + i + ".txt");
                processBuilder.redirectError(clientErrFile.toFile());
                processBuilder.redirectOutput(clientOutFile.toFile());
                process = processBuilder.start();
            } catch (Exception e) {
                testResult = new TestResult("Error while starting client: " + e);
                return;
            }

            try {
                process.waitFor(900_000, TimeUnit.MILLISECONDS);
                LOGGER.info("id: " + (id + 1) + "; Progress: " + (i + 1) * 100 / reqAmount + "%");
            } catch (InterruptedException e) {
                testResult = new TestResult("Client was terminated by timeout: " + e);
                return;
            }
        }
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
            Path clientOutFile = logDir.resolve("client_output_" + id + "__" + i + ".txt");
            File file = new File(String.valueOf(clientOutFile));
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String st;
                while ((st = bufferedReader.readLine()) != null)
                    if (st.contains("200 OK") || st.contains("Request succeeded (200)."))
                        results[i] = true;
            } catch (IOException e) {
                throw new TestErrorException("Can't find or read file: " + e);
            }
        }
        return IntStream.range(0, results.length).mapToObj(idx -> results[idx]).allMatch(result -> result);
    }
}
