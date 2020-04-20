package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class UT2ClientSideImpl implements UT2ClientSide {
    private ProcessBuilder processBuilder;
    private Process process;
    private OutputStreamWriter stdIn;

    private final Path errFile;
    private final Path outFile;

    public UT2ClientSideImpl(int id, Path logDir, UT2Mode ut2Mode, Configuration.Device clientDevice, Configuration.Device... serverDevices) throws TestErrorException {
        Path clientDir = Paths.get("").resolve(applicationProps.getProperty("ut2.client.dir"));
        Path clientConfigurationDir = Paths.get(clientDir + applicationProps.getProperty("ut2.client.configuration"));
        Configuration.writeClientConfiguration(ut2Mode, clientConfigurationDir, clientDevice, serverDevices);

        errFile = logDir.resolve("error_" + (id + 1) + ".txt");
        outFile = logDir.resolve("output_" + (id + 1) + ".txt");

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

    public void start() throws TestErrorException {
        try {
            process = processBuilder.start();
            stdIn = new OutputStreamWriter(process.getOutputStream());
        } catch (IOException e) {
            throw new TestErrorException("Error while starting client: " + e);
        }
    }

    public void clear() {
        if (process != null) {
            process.destroy();
        }
    }

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

    public boolean waitForClient(long millis) {
        boolean finished = false;
        try {
            finished = process.waitFor(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return finished && process.exitValue() == 0;
    }

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

    public boolean finishClientProcess(long millis) throws TestErrorException {
        sendCommand(ClientInterface.exit());
        return waitForClient(millis);
    }
}
