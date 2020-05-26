package one.transport.ut2.testing.entity;


import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractServer extends Thread {
    protected final Path logDir;
    protected TestResult testResult;

    protected int port;
    protected Map<Integer, byte[]> filesData;

    protected AbstractServer(Path logDir) {
        testResult = new TestResult();
        this.logDir = logDir;
    }

    public abstract void clear() throws TestErrorException;

    public void setFilesData(Map<Integer, byte[]> filesData) {
        this.filesData = filesData;
    }

    public TestResult getTestResult() {
        return testResult;
    }
}
