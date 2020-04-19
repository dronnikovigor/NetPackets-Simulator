package one.transport.ut2.testing.entity;

import one.transport.ut2.testing.tunnel.TunnelInterface;

class TestContext {
    private final Configuration configuration;
    private final TunnelInterface tunnelInterface;

    public TestContext(Configuration configuration,
                       TunnelInterface tunnelInterface) {
        this.configuration = configuration;
        this.tunnelInterface = tunnelInterface;
    }
}
