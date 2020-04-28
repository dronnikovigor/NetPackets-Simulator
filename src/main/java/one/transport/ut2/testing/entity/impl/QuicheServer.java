package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;
import one.transport.ut2.testing.utils.ProcessOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicheServer extends AbstractServer {
    private String[] cmd;
    private ProcessOutputStream outputGobbler;

    public QuicheServer(Configuration.Device device, Path logDir) throws TestErrorException {
        super(logDir);
        try {
            cmd = new String[]{"/bin/sh", "-c",
                    "cd " + applicationProps.getProperty("quiche.home.folder") + ";" +
                            " RUST_LOG=info " + applicationProps.getProperty("quiche.server.binary") +
                            " --cert " + applicationProps.getProperty("quic.certs.file") +
                            " --key " + applicationProps.getProperty("quic.key.file") +
                            " --listen " + InetAddress.getByAddress(device.getIpBytes()).getHostAddress() + ":" + device.udpPort +
                            " --root " + applicationProps.getProperty("temp.data.folder")
            };
            port = device.udpPort;
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }

    @Override
    public void run() {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            outputGobbler = new ProcessOutputStream(process.getErrorStream(), logDir.resolve("server_output.txt"));
            outputGobbler.start();
            testResult.success = true;
        } catch (IOException e) {
            testResult = new TestResult("Error while starting server: " + e);
        }
    }

    @Override
    public void clear() throws TestErrorException {
        try {
            Process findProcess = Runtime.getRuntime().exec("lsof -i");

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(findProcess.getInputStream()));
            String line;
            while ((line = stdInput.readLine()) != null) {
                if (line.contains(":" + port)) {
                    String[] values = line.split(" ");
                    for (String value : values) {
                        try {
                            int pid = Integer.parseInt(value);
                            Runtime.getRuntime().exec("kill " + pid);
                            return;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            outputGobbler.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new TestErrorException("Error while finishing server. PID not found!");
    }
}