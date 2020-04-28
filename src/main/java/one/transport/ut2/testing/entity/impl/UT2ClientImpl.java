package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractUT2Client;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.UT2Mode;

import java.nio.file.Path;

public class UT2ClientImpl extends AbstractUT2Client {

    public UT2ClientImpl(int id, Path logDir, UT2Mode ut2Mode, Configuration.Device clientDevice, Configuration.Device... serverDevices) throws TestErrorException {
        super(id, logDir, ut2Mode, clientDevice, serverDevices);
    }
}
