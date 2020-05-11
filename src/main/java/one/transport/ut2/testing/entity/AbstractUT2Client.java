package one.transport.ut2.testing.entity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractUT2Client implements UT2Client, CommonClient {
    private static final String ERROR_NAME = "error_%d.txt";
    private static final String OUTPUT_NAME = "output_%d.txt";

    private ProcessBuilder processBuilder;
    private Process process;
    private OutputStreamWriter stdIn;
    private final Path errFile;
    private final Path outFile;


    public AbstractUT2Client(int id, Path logDir, UT2Mode ut2Mode, Configuration.Device clientDevice, Configuration.Device... serverDevices) throws TestErrorException {
        Path clientDir = Paths.get("").resolve(applicationProps.getProperty("ut2.client.dir"));
        Path clientConfigurationDir = Paths.get(clientDir + applicationProps.getProperty("ut2.client.configuration"));
        Configuration.writeClientConfiguration(ut2Mode, clientConfigurationDir, clientDevice, serverDevices);

        errFile = logDir.resolve(String.format(ERROR_NAME, id + 1));
        outFile = logDir.resolve(String.format(OUTPUT_NAME, id + 1));

        try {
            Files.createFile(errFile);
            Files.createFile(outFile);

            processBuilder = new ProcessBuilder()
                    .directory(clientDir.toFile())
                    //.command("/bin/bash", "-c", "valgrind --log-file=\"" + logDir.toAbsolutePath().toString() + "/valgrind_report.txt\" ./build/client")
                    //todo add possibility to run without valgrind
                    .command("/bin/bash", "-c", "./build/client")
                    .redirectError(errFile.toFile())
                    .redirectOutput(outFile.toFile());
        } catch (IOException e) {
            throw new TestErrorException("Error while creating log files for client: " + e);
        }
    }

    @Override
    public void start() throws TestErrorException {
        try {
            process = processBuilder.start();
            stdIn = new OutputStreamWriter(process.getOutputStream());
        } catch (IOException e) {
            throw new TestErrorException("Error while starting client: " + e);
        }
    }

    @Override
    public void clear() {
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    public boolean waitForClient(long millis) {
        boolean finished = false;
        try {
            finished = process.waitFor(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return finished /*&& process.exitValue() == 0*/;
    }

    @Override
    public final void sendCommand(String command) throws TestErrorException {
        try {
            if (process.isAlive()) {
                stdIn.write(command);
                stdIn.flush();
            }
        } catch (IOException e) {
            throw new TestErrorException("Error while sending command to client: " + e);
        }
    }

    @Override
    public final void sendCommands(int id, String... commands) throws TestErrorException {
        try {
            for (String command : commands) {
                stdIn.write(command);
            }
            stdIn.flush();
        } catch (IOException e) {
            throw new TestErrorException("Error while sending command to client: " + e);
        }
    }

    @Override
    public boolean finishClientProcess(long millis) throws TestErrorException {
        sendCommand(ClientInterface.exit());
        return waitForClient(millis);
    }

    @Override
    public String getError() throws TestErrorException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(errFile.toFile()))) {
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    result.append(line).append(" \n");
                } else
                    break;
            }
        } catch (IOException e) {
            throw new TestErrorException("Error while reading from client file " + e);
        }
        return result.toString();
    }

    @Override
    public long getResultTime(int fileSize) throws TestErrorException {
        try (FileInputStream fileInputStream = new FileInputStream(outFile.toFile());
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.contains("requests for file "+ fileSize +" Kbytes completed in")) {
                    String[] lineByWord = line.split(" ");
                    int success_requests = Integer.parseInt(lineByWord[13]);
                    return (long) Double.parseDouble(lineByWord[10]);
                }
        } catch (IOException | NumberFormatException e) {
            throw new TestErrorException("Error while reading client output: " + e);
        }

        return -1;
    }

    @Override
    public boolean validateResponse(int fileSize) throws TestErrorException {
        try (FileInputStream fileInputStream = new FileInputStream(outFile.toFile());
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.contains("requests for file "+ fileSize +" Kbytes completed in")) {
                    String[] lineByWord = line.split(" ");
                    int total_requests = Integer.parseInt(lineByWord[2]);
                    int success_requests = Integer.parseInt(lineByWord[13]);
                    return success_requests == total_requests;
                }
        } catch (IOException | NumberFormatException e) {
            throw new TestErrorException("Error while reading client output: " + e);
        }

        return false;
    }
}
