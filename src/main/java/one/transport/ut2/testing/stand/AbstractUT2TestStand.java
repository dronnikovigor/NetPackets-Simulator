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
    protected UT2Mode ut2Mode;
    private final List<ClientProcess> clientProcesses = new ArrayList<>();

    protected UT2ServerSide[] serverSides;

    private Path clientDir;
    private Path clientConfigurationDir;

    @Override
    public void init(Configuration configuration, TunnelInterface tunnelInterface) throws TestErrorException {
        super.init(configuration, tunnelInterface);

        final Configuration.Device[] serversDevices = configuration.getServers();
        serverSides = new UT2ServerSide[serversDevices.length];
        for (int i = 0; i < serverSides.length; ++i) {
            Configuration.Device serverDevice = serversDevices[i];
            serverSides[i] = new UT2ServerSide(Executors.newSingleThreadExecutor(), ut2Mode);
            serverSides[i].initServer(serverDevice);

            ClusterHost2.Builder builder = new ClusterHost2.Builder();
            /* set all ip with client net addr */
            //todo improve
            byte[] ip = {10, 0, 0, 0};
            ip[3] = serverDevice.getHostAddr();
            builder.ip = IpUtils.ipAddrFromBytes(ip);
            /* */
            builder.instanceId = 0xFF & serverDevice.getHostAddr();
            if (ut2Mode == UT2Mode.UDP)
                builder.udpPort = serverSides[i].getBindUdpPort();
            else
                builder.tcpPort = serverSides[i].getBindTcpPort();
            builder.status = ClusterHostStatus.running;
            /* */
            ClusterHost2 host = new ClusterHost2(builder);
            ClusterUtils.hosts.put(host, true);
        }

        clientDir = Paths.get("").resolve(applicationProps.getProperty("ut2.client.dir"));
        clientConfigurationDir = Paths.get(clientDir + applicationProps.getProperty("ut2.client.configuration"));

        generateTestFiles(null);
    }

    protected void initClientProcess() throws TestErrorException {
        final ClientProcess clientProcess = new ClientProcess();
        clientProcess.start();
        clientProcesses.add(clientProcess);
    }

    protected String getError() {
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
    public void clear() throws TestErrorException {
        clientProcesses.forEach(clientProcess -> {
            if (clientProcess.process != null) {
                clientProcess.process.destroy();
            }
        });
        clientProcesses.clear();

        super.clear();

        for (ServerSide serverSide : serverSides) {
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

    protected boolean waitForClientProcess(int id, long millis) {
        boolean finished = false;
        try {
            finished = clientProcesses.get(id).process.waitFor(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return finished && clientProcesses.get(id).process.exitValue() == 0;
    }

    protected final void sendCommand(int id, String command) {
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

    protected final void writeClientConfiguration(Configuration.Device client, Configuration.Device... servers) throws TestErrorException {
        Configuration.writeClientConfiguration(ut2Mode, clientConfigurationDir, client, servers);
    }

    private class ClientProcess {
        private ProcessBuilder processBuilder;
        private Process process;
        private OutputStreamWriter stdIn;

        private final Path errFile;
        private final Path outFile;

        ClientProcess() throws TestErrorException {
            errFile = logDir.resolve("error_" + (clientProcesses.size() + 1) + ".txt");
            outFile = logDir.resolve("output_" + (clientProcesses.size() + 1) + ".txt");

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

        void start() throws TestErrorException {
            try {
                process = processBuilder.start();
                stdIn = new OutputStreamWriter(process.getOutputStream());
            } catch (IOException e) {
                throw new TestErrorException("Error while starting client: " + e);
            }
        }
    }

}
