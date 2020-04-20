package one.transport.ut2.testing.entity;

public interface UT2ClientSide {

    void start() throws TestErrorException;

    void clear();

    String getError() throws TestErrorException;

    boolean waitForClient(long millis);

    void sendCommand(String command) throws TestErrorException;

    void sendCommands(int id, String... commands) throws TestErrorException;

    boolean finishClientProcess(long millis) throws TestErrorException;
}
