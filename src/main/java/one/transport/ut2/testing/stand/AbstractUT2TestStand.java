package one.transport.ut2.testing.stand;

import one.transport.ut2.cluster.ClusterHostStatus;
import one.transport.ut2.testing.cluster.ClusterHost2;
import one.transport.ut2.testing.cluster.ClusterUtils;
import one.transport.ut2.testing.entity.*;
import one.transport.ut2.testing.entity.impl.UT2ServerSide;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.utils.IpUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractUT2TestStand extends AbstractTestStand {
    UT2Mode ut2Mode;
    private List<ClientProcess> clientProcesses = new ArrayList<>();

    @Override
    public void init(Configuration configuration, TunnelInterface tunnelInterface) {
        final Configuration.Device[] serversDevices = configuration.getServers();
        UT2ServerSide[] ut2ServerSides = new UT2ServerSide[serversDevices.length];
        for (int i = 0; i < ut2ServerSides.length; ++i) {
            Configuration.Device serverDevice = serversDevices[i];
            ut2ServerSides[i] = new UT2ServerSide(Executors.newSingleThreadExecutor(), ut2Mode);
            ut2ServerSides[i].initServer(serverDevice);

            ClusterHost2.Builder builder = new ClusterHost2.Builder();
            /* set all ip with client net addr */
            //todo improve
            byte[] ip = {10, 0, 0, 0};
            ip[3] = serverDevice.getHostAddr();
            builder.ip = IpUtils.ipAddrFromBytes(ip);
            /* */
            builder.instanceId = 0xFF & serverDevice.getHostAddr();
            if (ut2Mode == UT2Mode.UDP)
                builder.udpPort = ut2ServerSides[i].getBindUdpPort();
            else
                builder.tcpPort = ut2ServerSides[i].getBindTcpPort();
            builder.status = ClusterHostStatus.running;
            /* */
            ClusterHost2 host = new ClusterHost2(builder);
            ClusterUtils.hosts.put(host, true);
        }

        Path clientDir = Paths.get("").resolve(applicationProps.getProperty("ut2.client.dir"));
        Path clientConfigurationDir = Paths.get(clientDir + applicationProps.getProperty("ut2.client.configuration"));

        this.testContext = new TestContext(
                configuration,
                clientDir,
                clientConfigurationDir,
                ut2ServerSides,
                tunnelInterface,
                new ArrayList<>());
    }

    void initClientProcess() throws IOException {
        final ClientProcess clientProcess = new ClientProcess();
        clientProcess.start();
        clientProcesses.add(clientProcess);
    }

    String getError() {
        StringBuilder result = new StringBuilder();
        for (ClientProcess clientProcess : clientProcesses) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(clientProcess.errFile.toFile()))) {
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line != null) {
                        result.append(line).append(" \n");
                    } else
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }

    @Override
    public void clear() {
        clientProcesses.forEach(clientProcess -> {
            if (clientProcess.process != null) {
                clientProcess.process.destroy();
            }
        });
        clientProcesses.clear();

        super.clear();

        for (ServerSide serverSide : testContext.serverSides) {
            try {
                serverSide.clear();
            } catch (InterruptedException e) {
                //ignore :)
            }
        }

        ClusterUtils.hosts.clear();
    }

    protected boolean finishClientProcess(int id, long millis) {
        sendCommand(id, ClientInterface.exit());
        return waitForClientProcess(id, millis);
    }

    boolean waitForClientProcess(int id, long millis) {
        boolean finished = false;
        try {
            finished = clientProcesses.get(id).process.waitFor(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return finished && clientProcesses.get(id).process.exitValue() == 0;
    }

    final void sendCommand(int id, String command) {
        try {
            if (clientProcesses.get(id).process.isAlive()) {
                clientProcesses.get(id).stdIn.write(command);
                clientProcesses.get(id).stdIn.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected final void sendCommands(int id, String... commands) {
        try {
            for (String command : commands) {
                clientProcesses.get(id).stdIn.write(command);
            }
            clientProcesses.get(id).stdIn.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final void writeClientConfiguration(Configuration.Device client, Configuration.Device... servers) throws IOException {
        Configuration.writeClientConfiguration(ut2Mode, testContext.clientConfigFile, client, servers);
    }

    private class ClientProcess {
        private ProcessBuilder processBuilder;
        private Process process;
        private OutputStreamWriter stdIn;

        private Path errFile;
        private Path outFile;

        ClientProcess() throws IOException {
            errFile = logDir.resolve("error_" + (clientProcesses.size() + 1) + ".txt");
            outFile = logDir.resolve("output_" + (clientProcesses.size() + 1) + ".txt");

            Files.createFile(errFile);
            Files.createFile(outFile);

            processBuilder = new ProcessBuilder()
                    .directory(testContext.clientDir.toFile())
                    //.command("/bin/bash", "-c", "valgrind --log-file=\"" + logDir.toAbsolutePath().toString() + "/valgrind_report.txt\" ./build/client")
                    //todo add possibility to run without valgrind
                    .command("/bin/bash", "-c", "./build/client")
                    .redirectError(errFile.toFile())
                    .redirectOutput(outFile.toFile());
        }

        public void start() throws IOException {
            process = processBuilder.start();
            stdIn = new OutputStreamWriter(process.getOutputStream());
        }
    }

}
