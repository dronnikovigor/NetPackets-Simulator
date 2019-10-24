package one.transport.ut2.testing.stand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import one.transport.ut2.ntv.UT2Lib;
import one.transport.ut2.testing.ApplicationProperties;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.tunnel.PacketLoss;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.tunnel.jni.Tunnel;
import one.transport.ut2.testing.tunnel.jni.TunnelLib;
import one.transport.ut2.testing.utils.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;
import static org.junit.jupiter.api.Assertions.fail;

class TestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(TestStand.class);

    private static Tunnel tunnel;
    private static TunnelInterface tunnelInterface;
    private static Configuration configuration;

    private static final HashMap<String, List<TestContext.TestResult>> allTestsResults = new HashMap<>();

    @BeforeAll
    static void init() throws IOException {
        ApplicationProperties.init();

        File configurationFile =
                new File(TestStand.class.getResource(applicationProps.getProperty("configuration.file")).getFile());
        configuration = Configuration.parseConfiguration(configurationFile);

        FileUtils.delete(Paths.get(applicationProps.getProperty("log.dir")));

        if (configuration == null) {
            LOGGER.error("Can't parse configuration file!!!");
            fail();
        }

        /* load native ut2 */
        UT2Lib.load(false);

        /* load tunnel library */
        TunnelLib.load();

        /* initializing tunnel */
        tunnel = new Tunnel();
        boolean success = tunnel.init(configuration.tunnelDevice);
        tunnelInterface = new TunnelInterface(tunnel, configuration.devices);
        LOGGER.info("result of tunnel init: " + success);

        if (!success) {
            fail();
        }
    }

    @AfterAll
    static void finish() {
        tunnel.stop();

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonWriter jsonWriter = gson.newJsonWriter(
                    new FileWriter(Paths.get(applicationProps.getProperty("log.dir")).resolve("results.json").toFile()));
            gson.toJson(allTestsResults, HashMap.class, jsonWriter);
            jsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void PureTcpDataTransferTest() {
        executeTest(new PureTcpDataTransferTestStand());
    }

    @Test
    void Ut2UdpDataTransferTest() {
        executeTest(new UT2UdpDataTransferTestStand());
    }

    @Test
    void QuicDataTransferTest() {
        executeTest(new QuicDataTransferTestStand());
    }

    private void executeTest(AbstractTestStand test) {
        LOGGER.info(test.getClass().getSimpleName() + " started");
        TestContext.TestResult testResult = null;
        for (int bandwidth : configuration.bandwidths) {
            for (PacketLoss.LossParams lossParams : configuration.lossParams) {
                for (int speed : configuration.speeds) {
                    for (double speedRate : configuration.speedRates) {
                        for (int fileSize : configuration.fileSizes) {
                            tunnelInterface.bandwidth = bandwidth;
                            tunnelInterface.lossParams = lossParams;
                            tunnelInterface.speed = speed;
                            tunnelInterface.speedRate = speedRate;
                            tunnelInterface.start();

                            try {
                                LOGGER.info("Test case started: [Bandwidth = " + bandwidth + "ms; FileSize = " +
                                        fileSize + "kb; PL = " + lossParams.getName() + "]");
                                test.init(configuration, tunnelInterface);
                                testResult = test.runTest(fileSize);

                                /* waiting while all packets from client/server reach dst */
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                testResult = new TestContext.TestResult(Arrays.toString(e.getStackTrace()), 0,
                                        bandwidth, fileSize, configuration.reqAmount, lossParams, null);
                            } finally {
                                allTestsResults.computeIfAbsent(test.getClass().getSimpleName(), k ->
                                        new ArrayList<>()).add(testResult);
                                LOGGER.info("Test case finished with time " + testResult.resultTime + "ms");
                                test.clear();
                            }
                            tunnelInterface.stop();
                        }
                    }
                }
            }
        }
        LOGGER.info(test.getClass().getSimpleName() + " finished");
    }
}
