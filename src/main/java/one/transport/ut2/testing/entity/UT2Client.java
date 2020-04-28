package one.transport.ut2.testing.entity;

public interface UT2Client {

    void start() throws TestErrorException;

    boolean waitForClient(long millis);

    void sendCommand(String command) throws TestErrorException;

    void sendCommands(int id, String... commands) throws TestErrorException;

    boolean finishClientProcess(long millis) throws TestErrorException;

    long getResultTime(int fileSize) throws TestErrorException;
}
