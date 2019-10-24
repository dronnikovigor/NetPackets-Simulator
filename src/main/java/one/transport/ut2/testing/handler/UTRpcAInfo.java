package one.transport.ut2.testing.handler;

import one.transport.ut2.UT2PeerCtx;
import one.transport.ut2.UT2Rpc0Handler;
import one.transport.ut2.UT2Rpc0Input;
import one.transport.ut2.UT2Rpc0Output;
import one.transport.ut2.utils.message.MessageBuf;
import one.transport.ut2.utils.message.MessageWriter;

public class UTRpcAInfo implements UT2Rpc0Handler {
    private static final MessageBuf INSTANCE = new MessageBuf("instance");

    private final long instanceId;

    public UTRpcAInfo(long instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public UT2Rpc0Input handle(short streamId, UT2PeerCtx ctx, UT2Rpc0Output output) {
        return new Handler(output);
    }

    private class Handler implements UT2Rpc0Input {
        final UT2Rpc0Output output;

        Handler(UT2Rpc0Output output) {
            this.output = output;
        }

        @Override
        public void handle(byte[] bytes) {
            output.send(
                    new MessageWriter()
                            .putLong(INSTANCE, instanceId)
                            .toByteArray()
            );
        }
    }
}
