package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.*;
import one.transport.ut2.ntv.UT2Server;
import one.transport.ut2.ntv.UT2ServerArgs;
import one.transport.ut2.ntv.UT2ZeroServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.UT2ServerSide;
import one.transport.ut2.testing.entity.UT2Mode;
import one.transport.ut2.testing.handler.OptClusterHandler;
import one.transport.ut2.testing.handler.UTRpcAInfo;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

public class UT2ServerSideImpl implements UT2ServerSide {
    private final Executor executor;
    private final UT2Mode ut2Mode;

    private byte instanceId;

    private UT2Server server;
    private UT2ServerHandler handler;
    private OptClusterHandler clusterHandler;

    public UT2ServerSideImpl(Executor executor, UT2Mode ut2Mode) {
        this.executor = executor;
        this.ut2Mode = ut2Mode;
    }

    @Override
    public void setServerHandler(UT2Pipe2Handler serverHandler) {
        UT2ZeroServer zeroServer = new UT2ZeroServer();
        handler.setPipeHandler(new SecureUT2PipeHandler(zeroServer)
                .addHandler("handler", serverHandler));
    }

    @Override
    public void setServerHandler(UT2ZeroServer zeroServer, UT2Pipe2Handler serverHandler) {
        handler.setPipeHandler(new SecureUT2PipeHandler(zeroServer)
                .addHandler("handler", serverHandler));
    }

    @Override
    public void initServer(@NotNull Configuration.Device serverDevice) throws TestErrorException {
        instanceId = serverDevice.getHostAddr();

        server = new UT2Server();
        UT2ServerArgs serverArgs = new UT2ServerArgs();

        //initializing args
        serverArgs.agent.concurrency = 1;

        if (ut2Mode == UT2Mode.UDP) {
            serverArgs.udp.enabled = true;
            //will select free port by itself
            try {
                serverArgs.udp.bindAddr.setHost(InetAddress.getByAddress(serverDevice.getIpBytes()));
            } catch (UnknownHostException e) {
                throw new TestErrorException("Unknown host: " + e);
            }
            serverArgs.udp.bindAddr.setPort(serverDevice.udpPort);
            serverArgs.udp.sender.concurrency = 1;
            serverArgs.udp.sender.socketBufferSize = 8 * 1024 * 1024;
            serverArgs.udp.receiver.concurrency = 1;
            serverArgs.udp.receiver.socketBufferSize = 8 * 1024 * 1024;
        } else {
            serverArgs.tcp.enabled = true;
            serverArgs.tcp.binding.enabled = true;
            try {
                serverArgs.tcp.binding.bindAddr.setHost(InetAddress.getByAddress(serverDevice.getIpBytes()));
            } catch (UnknownHostException e) {
                throw new TestErrorException("Unknown host: " + e);
            }
            serverArgs.tcp.binding.bindAddr.setPort(serverDevice.tcpPort);
        }

        server.config(serverArgs);
        server.bind();
        server.start();

        handler = new UT2ServerHandler(server, executor, 0);

        /* opt handler init */
        clusterHandler = new OptClusterHandler();
        clusterHandler.start();
        handler.setOptHandler(new MultiUT2OptHandler().
                addHandler("getCluster.request", clusterHandler));

        /* */
        handler.setRpc0Handler(new MultiUT2Rpc0Handler()
                .addHandler(Streams.INFO_STREAM, new UTRpcAInfo(0xFF & serverDevice.getHostAddr())));

        handler.start();

        //set server port to params
        if (ut2Mode == UT2Mode.UDP)
            serverDevice.udpPort = getBindUdpPort();
        else
            serverDevice.tcpPort = getBindTcpPort();
    }

    @Override
    public byte getInstanceId() {
        return instanceId;
    }

    @Override
    public int getBindUdpPort() {
        return server.getBindUdpPort(0);
    }

    @Override
    public int getBindTcpPort() {
        return server.getBindTcpPort(0);
    }

    @Override
    public void clear() throws InterruptedException {
        clusterHandler.stop();
        handler.stop();
        //closing handler and server
        handler.waitForStop();
        server.stop();
    }

}