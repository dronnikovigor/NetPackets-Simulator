package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.UT2Mode;

public class UT2UdpDataTransferTestStand extends UT2DataTransferTestStand {
    public UT2UdpDataTransferTestStand() {
        ut2Mode = UT2Mode.UDP;
        packetStat = true;
    }
}
