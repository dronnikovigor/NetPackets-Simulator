package one.transport.ut2.testing.stand.impl;

import one.transport.ut2.testing.entity.UT2Mode;
import one.transport.ut2.testing.stand.AbstractUT2TestStand;

public class UT2UdpDataTransferTestStand extends AbstractUT2TestStand {
    public UT2UdpDataTransferTestStand() {
        ut2Mode = UT2Mode.UDP;
        packetStat = true;
    }
}
