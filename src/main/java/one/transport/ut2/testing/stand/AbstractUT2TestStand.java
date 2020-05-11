package one.transport.ut2.testing.stand;

import one.transport.ut2.UT2PeerCtx;
import one.transport.ut2.UT2Pipe2Handler;
import one.transport.ut2.UT2PipeInput;
import one.transport.ut2.UT2PipeOutput;
import one.transport.ut2.cluster.ClusterHostStatus;
import one.transport.ut2.testing.cluster.ClusterHost2;
import one.transport.ut2.testing.cluster.ClusterUtils;
import one.transport.ut2.testing.entity.*;
import one.transport.ut2.testing.entity.impl.UT2ClientImpl;
import one.transport.ut2.testing.entity.impl.UT2ServerSideImpl;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractUT2TestStand extends AbstractTestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractUT2TestStand.class);
    private List<AbstractUT2Client> clientProcesses;
    private UT2ServerSide[] serverSides;
    protected UT2Mode ut2Mode;

    private volatile Map<Integer, byte[]> filesData;
    private final Random rnd = new Random();
    private final Object syncObj = new Object();

    private UT2ServerHandler requestsHandler;

    @Override
    public void init(Configuration configuration, TunnelInterface tunnelInterface) throws TestErrorException {
        super.init(configuration, tunnelInterface);

        final Configuration.Device[] serversDevices = configuration.getServers();
        serverSides = new UT2ServerSideImpl[serversDevices.length];
        clientProcesses = new ArrayList<>();
        for (int i = 0; i < serverSides.length; ++i) {
            Configuration.Device serverDevice = serversDevices[i];
            serverSides[i] = new UT2ServerSideImpl(Executors.newSingleThreadExecutor(), ut2Mode);
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

        filesData = new HashMap<>();
        generateTestFiles(null);
    }

    @Override
    public List<TestResult> runTest() throws TestErrorException {
        List<TestResult> testResults = super.runTest();

        UT2ServerSide ut2ServerSide = null;
        for (UT2ServerSide server : serverSides) {
            if (server.getInstanceId() == serverId) {
                ut2ServerSide = server;
                break;
            }
        }
        if (ut2ServerSide == null) {
            throw new TestErrorException("UT2Server in null");
        }

        for (Configuration.Device clientDevice : configuration.getClients()) {
            initClientProcess(clientDevice, configuration.getServers());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int fileSize : configuration.fileSizes) {
            requestsHandler = new UT2ServerHandler();
            ut2ServerSide.setServerHandler(requestsHandler);

            executeTest(fileSize);
        }

        for (int i = 0; i < configuration.getClients().length; i++) {
            if (!clientProcesses.get(i).finishClientProcess(60_000))
                throw new TestErrorException("Client terminated: " + getClientsErrors());
        }

        for (int fileSize : configuration.fileSizes) {
            boolean validated = true;
            for (UT2Client clientProcess : clientProcesses) {
                if (!clientProcess.validateResponse(fileSize)) {
                    validated = false;
                    break;
                }
            }
            if (validated) {
                long sum = 0;
                for (UT2Client clientProcess : clientProcesses) {
                    sum += clientProcess.getResultTime(fileSize);
                }

                testResults.add(setUpTestResult(fileSize, validated, sum));
            } else
                testResults.add(setUpTestResult(fileSize, validated, 0));
        }

        clientProcesses.forEach(AbstractUT2Client::clear);
        clientProcesses.clear();

        return testResults;
    }

    private void executeTest(int fileSize) throws TestErrorException {
        List<Thread> responseThreads = new ArrayList<>();

        for (int i = 0; i < configuration.getClients().length; i++) {
            clientProcesses.get(i).sendCommand(ClientInterface.sendCommand(configuration.reqAmount,
                    fileSize * 1024, Paths.get(applicationProps.getProperty("temp.data.folder") + "/" + fileSize + "KB")));

            Thread responseThread = new Thread(() -> {
                while (requestsHandler.lastRequestId != configuration.reqAmount - 1) {
                    synchronized (syncObj) {
                        try {
                            syncObj.wait(60_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            responseThread.start();
            responseThreads.add(responseThread);
        }

        for (int i = 0; i < responseThreads.size(); i++) {
            Thread responseThread = responseThreads.get(i);
            try {
                responseThread.join(60_000);
                LOGGER.info("Progress: " + (i + 1) * 100 / responseThreads.size() + "%");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private TestResult setUpTestResult(int fileSize, boolean success, long time) {
        TestResult testResult = new TestResult();
        testResult.rtt = tunnelInterface.rtt;
        testResult.fileSize = fileSize;
        testResult.requests = configuration.reqAmount;
        testResult.lossParams = tunnelInterface.lossParams;
        testResult.bandwidth = tunnelInterface.bandwidth;
        testResult.speedRate = tunnelInterface.speedRate;
        testResult.congestionControlWindow = tunnelInterface.getCongestionControlWindowCapacity();

        testResult.success = success;
        testResult.resultTime = time / clientProcesses.size();
        testResult.packetLoss.fromClients = tunnelInterface.statistic.clients.getPacketLoss();
        testResult.packetLoss.fromServers = tunnelInterface.statistic.servers.getPacketLoss();
        return testResult;
    }

    @Override
    public void clear() throws TestErrorException {
        super.clear();

        for (UT2ServerSide UT2ServerSide : serverSides) {
            try {
                UT2ServerSide.clear();
            } catch (InterruptedException ignored) {
                //ignore :)
            }
        }

        ClusterUtils.hosts.clear();
    }

    @Override
    protected void createFile(String path, int size, String serverAddress) throws TestErrorException {
        byte[] fileData = new byte[size * 1024];
        rnd.nextBytes(fileData);
        filesData.put(size * 1024, fileData);

        try {
            Files.createFile(Paths.get(path));
            FileOutputStream fileOutputStream = new FileOutputStream(new File(path));
            fileOutputStream.write(fileData);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new TestErrorException("Error while generating files: " + e);
        }
    }

    private void initClientProcess(Configuration.Device clientDevice, Configuration.Device... serverDevices) throws TestErrorException {
        final AbstractUT2Client clientProcess = new UT2ClientImpl(clientProcesses.size(), logDir, ut2Mode, clientDevice, serverDevices);
        clientProcess.start();
        clientProcesses.add(clientProcess);
    }

    private String getClientsErrors() throws TestErrorException {
        StringBuilder result = new StringBuilder();
        for (AbstractUT2Client clientProcess : clientProcesses) {
            String clientProcessError = clientProcess.getError();
            if (!clientProcessError.isEmpty())
                result.append(clientProcessError).append("\n");
        }
        return result.toString();
    }

    public class UT2ServerHandler implements UT2Pipe2Handler {
        volatile int lastRequestId;
        volatile boolean wrongRequestedFileSize;

        @Override
        public UT2PipeInput createPipe(String s, UT2PeerCtx ut2PeerCtx, UT2PipeOutput ut2PipeOutput) {
            return new UT2PipeInput() {
                @Override
                public void onChunk(byte[] bytes) {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    //todo don't change bytes order in client
//                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    int responseSize = buffer.getInt();
                    wrongRequestedFileSize = filesData.containsKey(responseSize);
                    lastRequestId = buffer.getInt();
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                    ut2PipeOutput.writeChunk(filesData.get(responseSize));
                }

                @Override
                public void onClose() {
                }
            };
        }
    }
}
