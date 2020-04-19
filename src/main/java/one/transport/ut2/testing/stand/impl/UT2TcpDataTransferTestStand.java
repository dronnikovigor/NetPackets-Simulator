package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.UT2Mode;

class UT2TcpDataTransferTestStand extends UT2DataTransferTestStand {
    public UT2TcpDataTransferTestStand() {
        ut2Mode = UT2Mode.TCP;
    }
}
