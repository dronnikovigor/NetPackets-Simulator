package one.transport.ut2.testing.entity;

import one.transport.ut2.UT2Pipe2Handler;
import one.transport.ut2.ntv.UT2ZeroServer;
import org.jetbrains.annotations.NotNull;

public interface UT2ServerSide {
    void initServer(@NotNull Configuration.Device serverDevice) throws TestErrorException;

    byte getInstanceId();

    int getBindUdpPort();

    int getBindTcpPort();

    void clear() throws InterruptedException;

    void setServerHandler(UT2Pipe2Handler serverHandler);

    void setServerHandler(UT2ZeroServer zeroServer, UT2Pipe2Handler serverHandler);
}
