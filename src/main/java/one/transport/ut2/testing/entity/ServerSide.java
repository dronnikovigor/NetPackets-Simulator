package one.transport.ut2.testing.entity;

import org.jetbrains.annotations.NotNull;

public interface ServerSide {

    void initServer(@NotNull Configuration.Device serverDevice);

    int getBindUdpPort();

    int getBindTcpPort();

    void clear() throws InterruptedException;
}
