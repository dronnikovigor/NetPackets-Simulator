package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicheServer extends AbstractServer {
    private String[] cmd;

    public QuicheServer(Configuration.Device device, Path logDir) throws TestErrorException {
        super(logDir);
        try {
            cmd = new String[]{"/bin/sh", "-c",
                    "cd " + applicationProps.getProperty("quiche.home.folder") + ";" +
                            applicationProps.getProperty("quiche.server.binary") +
                            " --cert " + applicationProps.getProperty("quiche.cert.file") +
                            " --key " + applicationProps.getProperty("quiche.key.file") +
                            " --listen " + InetAddress.getByAddress(device.getIpBytes()).getHostAddress() + ":" + device.udpPort +
                            " --root " + applicationProps.getProperty("temp.data.folder")};
            port = device.udpPort;
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().exec(cmd);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new TestErrorException("Error while finishing server. PID not found!");
    }
}

//    public QuicheServer(Configuration.Device device, Path logDir) throws TestErrorException {
//        super(logDir);
//        try {
//            processBuilder = new ProcessBuilder()
//                    .directory(logDir.toFile())
//                    .command(
//                            applicationProps.getProperty("quiche.server.binary"),
//                            " --cert " + applicationProps.getProperty("quiche.cert.file"),
//                            " --key " + applicationProps.getProperty("quiche.key.file"),
//                            " --listen " + InetAddress.getByAddress(device.getIpBytes()).getHostAddress() + ":" + device.udpPort,
//                            " --root " + applicationProps.getProperty("quic.server.data.folder")
//                    );
//            port = device.udpPort;
//        } catch (UnknownHostException e) {
//            throw new TestErrorException("Unknown Host: " + e);
//        }
//    }
//
//    @Override
//    public void run() {
//        try {
//            process = processBuilder.start();
//            testResult.success = true;
//        } catch (IOException e) {
//            testResult = new TestResult("Error while starting server: " + e);
//        }
//    }
//
//    @Override
//    public void clear() {
//        if (process != null && process.isAlive())
//            process.destroy();
//    }
