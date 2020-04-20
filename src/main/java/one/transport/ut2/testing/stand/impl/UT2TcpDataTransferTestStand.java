package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.UT2Mode;
import one.transport.ut2.testing.stand.AbstractUT2TestStand;

class UT2TcpDataTransferTestStand extends AbstractUT2TestStand {
    public UT2TcpDataTransferTestStand() {
        ut2Mode = UT2Mode.TCP;
    }
}
