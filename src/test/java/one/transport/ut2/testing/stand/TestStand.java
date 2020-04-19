package one.transport.ut2.testing.stand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import one.transport.ut2.ntv.UT2Lib;
import one.transport.ut2.testing.ApplicationProperties;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestResult;
import one.transport.ut2.testing.stand.impl.*;
import one.transport.ut2.testing.tunnel.PacketLoss;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.tunnel.jni.Tunnel;
import one.transport.ut2.testing.tunnel.jni.TunnelLib;
import one.transport.ut2.testing.utils.FileUtils;
import org.junit.jupiter.api.*;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestStand {
    private final static Logger LOGGER = LoggerFactory.getLogger(TestStand.class);

    private static Tunnel tunnel;
    private static TunnelInterface tunnelInterface;
    private static Configuration configuration;

    private static final HashMap<String, List<TestResult>> allTestsResults = new HashMap<>();

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
    @Order(1)
    void QuicDataTransferTest() throws Exception {
        executeTest(new QuicDataTransferTestStand());
    }

    @Test
    @Order(2)
    void QuicheDataTransferTest() throws Exception {
        executeTest(new QuicheDataTransferTestStand());
    }

    @Test
    @Order(3)
    void QuicQuicheDataTransferTest() throws Exception {
        executeTest(new QuicQuicheDataTransferTestStand());
    }

    @Test
    @Order(4)
    void Ut2UdpDataTransferTest() throws Exception {
        executeTest(new UT2UdpDataTransferTestStand());
    }

    @Test
    @Order(5)
    void PureTcpDataTransferTest() throws Exception {
        executeTest(new PureTcpDataTransferTestStand());
    }

    private void executeTest(AbstractTestStand test) throws Exception {
        LOGGER.info(test.getClass().getSimpleName() + " started");
        TestResult testResult = null;
        int progress = 1;
        int totalProgress = configuration.rtts.length
                * configuration.lossParams.length
                * configuration.bandwidths.length
                * configuration.speedRates.length
                * configuration.congestionControlWindows.length
                * configuration.fileSizes.length;
        for (int rtt : configuration.rtts) {
            for (PacketLoss.LossParams lossParams : configuration.lossParams) {
                for (int bandwidth : configuration.bandwidths) {
                    for (double speedRate : configuration.speedRates) {
                        for (int congestionControlWindow : configuration.congestionControlWindows) {
                            for (int fileSize : configuration.fileSizes) {
                                tunnelInterface.rtt = rtt;
                                tunnelInterface.lossParams = lossParams;
                                tunnelInterface.bandwidth = bandwidth;
                                tunnelInterface.speedRate = speedRate;
                                tunnelInterface.setCongestionControlWindowCapacity(congestionControlWindow);
                                tunnelInterface.packetStat = test.packetStat;
                                tunnelInterface.start();

                                try {
                                    LOGGER.info("Test case started: \n" +
                                            "[FileSize = " + fileSize + "kb; Config = " + tunnelInterface + "]");
                                    test.init(configuration, tunnelInterface);
                                    testResult = test.runTest(fileSize);
                                } catch (Exception e) {
                                    testResult = new TestResult(
                                            e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()),
                                            0,
                                            fileSize,
                                            bandwidth,
                                            rtt,
                                            configuration.reqAmount,
                                            speedRate,
                                            congestionControlWindow,
                                            lossParams,
                                            null);
                                } finally {
                                    allTestsResults.computeIfAbsent(test.getClass().getSimpleName(), k ->
                                            new ArrayList<>()).add(testResult);
                                    LOGGER.info("Test case finished with time " + testResult.resultTime + "ms, result: " + testResult.success);
                                    LOGGER.info("Test progress: " + progress++ + " / " + totalProgress);
                                    if (!testResult.success)
                                        LOGGER.error("Error: " + testResult.error);
                                    test.clear();
                                }
                                tunnelInterface.stop();
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info(test.getClass().getSimpleName() + " finished");
    }
}
