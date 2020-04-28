package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.Path;

public class PureTcpServer extends AbstractServer {
    private final static Logger LOGGER = LoggerFactory.getLogger(PureTcpServer.class);
    private ServerSocket serverSocket;

    public PureTcpServer(Configuration.Device serverDevice, Path logDir) throws TestErrorException {
        super(logDir);
        try {
            serverSocket = new ServerSocket();
            SocketAddress serverAddr =
                    new InetSocketAddress(InetAddress.getByAddress(serverDevice.getIpBytes()), 0);
            serverSocket.bind(serverAddr);
            serverDevice.tcpPort = serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new TestErrorException("Error while opening socket: " + e);
        }
    }

    @Override
    public void run() {
        testResult.success = true;
        while (!isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    LOGGER.info("New connection opened: " + socket.getRemoteSocketAddress());
                    final byte[] buffer = new byte[1024];
                    try {
                        InputStream is = socket.getInputStream();
                        OutputStream os = socket.getOutputStream();
                        DataInputStream dis = new DataInputStream(
                                new BufferedInputStream(is)
                        );
                        DataOutputStream dos = new DataOutputStream(
                                new BufferedOutputStream(os)
                        );

                        while (!PureTcpServer.this.isInterrupted() && socket.isConnected()) {
                            try {
                                dis.readFully(buffer);
                                String request = trimZeros(new String(buffer));
                                try {
                                    int fileSize = Integer.parseInt(request);
                                    dos.write(filesData.get(fileSize));
                                    dos.flush();
                                } catch (NumberFormatException ignored) {

                                }
                            } catch (EOFException e) {
                                //TODO fix exception
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {

    }

    static String trimZeros(String str) {
        int pos = str.indexOf(0);
        return pos == -1 ? str : str.substring(0, pos);
    }
}