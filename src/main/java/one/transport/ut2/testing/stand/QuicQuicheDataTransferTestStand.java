package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicQuicheDataTransferTestStand extends AbstractQuicDataTransferTestStand {

    @Override
    void initServer(Configuration.Device device) throws IOException {
        serverThread = new ServerThread(device);
        serverThread.start();
    }

    protected class ServerThread extends AbstractServerThread {
        private String[] cmd;

        private int port;

        ServerThread(Configuration.Device device) throws IOException {
            port = device.udpPort;
            cmd = new String[]{"/bin/sh", "-c",
                    "cd " + applicationProps.getProperty("quiche.home.folder") + ";" +
                            " cargo run" +
                            " --manifest-path=" + applicationProps.getProperty("quiche.manifest.path") +
                            " --bin quiche-server --" +
                            " --cert " + applicationProps.getProperty("quiche.cert.file") +
                            " --key " + applicationProps.getProperty("quiche.key.file") +
                            " --listen " + InetAddress.getByAddress(device.getIpBytes()).getHostAddress() + ":" + device.udpPort +
                            " --root " + applicationProps.getProperty("quic.server.data.folder") + "/www.example.org"};
        }

        @Override
        public void run() {
            try {
                process = Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                testResult = new TestContext.TestResult("Error while starting server: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        @Override
        public void clear() {
            try {
                Process findProcess = Runtime.getRuntime().exec("lsof -i");

                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(findProcess.getInputStream()));
                String line;
                while ((line = stdInput.readLine()) != null) {
                    if (line.contains(":"+port)) {
                        int firstIndex = line.indexOf(" ");
                        int secondIndex = line.indexOf(" ", firstIndex+1);
                        String result = line.substring(firstIndex, secondIndex);

                        Runtime.getRuntime().exec("kill " + result);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
