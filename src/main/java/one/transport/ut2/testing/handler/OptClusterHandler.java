package one.transport.ut2.testing.handler;

import one.transport.ut2.UT2Opt2Handler;
import one.transport.ut2.UT2OptInput;
import one.transport.ut2.UT2OptOutput;
import one.transport.ut2.UTTimer;
import one.transport.ut2.ntv.UT2Addr;
import one.transport.ut2.ntv.UT2Deflate;
import one.transport.ut2.testing.ExecutorAction;
import one.transport.ut2.testing.cluster.ClusterHost2;
import one.transport.ut2.testing.cluster.ClusterUtils;
import one.transport.ut2.utils.concurrency.ChainExecutor;
import one.transport.ut2.utils.concurrency.ChainExecutorBuilder;
import one.transport.ut2.utils.io.MemoryOutputStream;
import one.transport.ut2.utils.message.Message;
import one.transport.ut2.utils.message.MessageBuf;
import one.transport.ut2.utils.message.MessageWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import static one.transport.ut2.dict.UTResolverConstants.GET_CLUSTER_RESPONSE;

public class OptClusterHandler implements UT2Opt2Handler {
    private static final MessageBuf VERSION = new MessageBuf("v");
    private static final MessageBuf REQUEST_ID = new MessageBuf("#");

    private static final MessageBuf ARGS = new MessageBuf("a");
    private static final MessageBuf HOSTS = new MessageBuf("h");
    private static final MessageBuf STATUS = new MessageBuf("s");

    private final ChainExecutor sequencer = ChainExecutorBuilder.create(Executors.newSingleThreadExecutor());
    private final UTTimer timer = new UTTimer(sequencer);
    private final ExecutorAction doUpdate = new ExecutorAction(sequencer, new DoUpdate()) {
        @Override
        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                //todo
            }
        }
    };

    private volatile MessageBuf zStatus = MessageBuf.EMPTY;

    public void start() {
        doUpdate.execute();
        timer.scheduleWithFixedDelay(doUpdate, 5000);
    }

    public void stop() {
        timer.cancel();
    }

    @Override
    public UT2OptInput handle(String code, UT2Addr addr, UT2OptOutput output) {
        return new Handler(code, output);
    }

    private class DoUpdate implements Runnable {
        @Override
        public void run() {
            byte[] body = new MessageWriter()
                    .put(ARGS, args())
                    .put(HOSTS, hosts())
                    .toByteArray();
            byte[] zBody = UT2Deflate.deflateFully(body);
            zStatus = MessageBuf.wrap(zBody);
        }

        byte[] hosts() {
            MemoryOutputStream out = new MemoryOutputStream();
            MessageWriter writer = new MessageWriter();
            for (ClusterHost2 host : ClusterUtils.getHosts()) {
                if (host.weight <= 0) continue;
                if (host.udpPort <= 0 && host.tcpPort <= 0) continue;
                if (host.status == null) continue;
                writer.reset();
                switch (host.status) {
                    case stopped:
                    case suspended:
                        writer.putBool(new MessageBuf("s"), true);
                    case running:
                        break;
                    default:
                        continue;
                }
                try {
                    InetAddress address = InetAddress.getByName(host.ip);
                    writer.put(new MessageBuf("a"), address.getAddress());
                } catch (UnknownHostException e) {
                    continue;
                }
                writer.putLong(new MessageBuf("i"), host.instanceId);
                if (host.udpPort > 0) {
                    writer.putShort(new MessageBuf("u"), (short) host.udpPort);
                }
                if (host.tcpPort > 0) {
                    writer.putShort(new MessageBuf("t"), (short) host.tcpPort);
                }
                writer.putInt(new MessageBuf("w"), host.weight);
                out.writeVUInt(writer.size());
                out.write(writer.toByteArray());
            }
            return out.toByteArray();
        }

        byte[] args() {
            MemoryOutputStream out = new MemoryOutputStream();
            out.writeVUInt(0);
            return out.toByteArray();
        }
    }

    private class Handler implements UT2OptInput {
        final String code;
        final UT2OptOutput output;

        Handler(String code, UT2OptOutput output) {
            this.code = code;
            this.output = output;
        }

        @Override
        public void handle(byte[] bytes) {
            int reqId = 0;

            if (bytes != null) {
                Message args = new Message(bytes);
                if (args.verify()) {
                    reqId = args.findOrEmpty(REQUEST_ID).asInt(0);
                }
            }

            // opt version == 2
            MessageWriter writer = new MessageWriter();
            writer.writeBytes(GET_CLUSTER_RESPONSE);
            writer.write('\n');
            writer
                    .put(STATUS, zStatus)
                    .putInt(REQUEST_ID, reqId);

            output.send(writer.toByteArray(), true);
        }
    }
}
