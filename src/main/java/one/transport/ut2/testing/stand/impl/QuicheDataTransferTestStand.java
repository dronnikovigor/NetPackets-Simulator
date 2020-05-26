package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.AbstractClient;
import one.transport.ut2.testing.entity.AbstractServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.impl.QuicheClient;
import one.transport.ut2.testing.entity.impl.QuicheServer;
import one.transport.ut2.testing.stand.AbstractQuicDataTransferTestStand;

public class QuicheDataTransferTestStand extends AbstractQuicDataTransferTestStand {

    @Override
    protected AbstractServer initServer(Configuration.Device device) throws TestErrorException {
        return new QuicheServer(device, logDir);
    }

    @Override
    protected AbstractClient initClient(Configuration.Device clientDevice, Configuration.Device serverDevice) throws TestErrorException {
        return new QuicheClient(clientDevice, serverDevice, clientThreads.size(), logDir);
    }
}
