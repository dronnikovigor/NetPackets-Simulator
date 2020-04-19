package one.transport.ut2.testing.stand;

import one.transport.ut2.UT2PeerCtx;
import one.transport.ut2.UT2Pipe2Handler;
import one.transport.ut2.UT2PipeInput;
import one.transport.ut2.UT2PipeOutput;
import one.transport.ut2.testing.entity.TestErrorException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class AbstractFileSendingTestStand extends AbstractUT2TestStand {
    private volatile byte[] fileData;
    protected final Object syncObj = new Object();

    @Override
    protected void createFile(String path, int size, String serverAddress) throws TestErrorException {
        fileData = new byte[size * 1024];
        File file = new File(path);
        try (FileWriter fileWriter = new FileWriter(file, false);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(new String(fileData));
        } catch (IOException e) {
            throw new TestErrorException("Error while generating files: " + e);
        }
    }

    public class ServerHandler implements UT2Pipe2Handler {
        public volatile int lastRequestId;
        volatile boolean wrongRequestedFileSize;

        @Override
        public UT2PipeInput createPipe(String s, UT2PeerCtx ut2PeerCtx, UT2PipeOutput ut2PipeOutput) {
            return new UT2PipeInput() {
                @Override
                public void onChunk(byte[] bytes) {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    //todo don't change bytes order in client
//                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    int responseSize = buffer.getInt();
                    wrongRequestedFileSize = responseSize != fileData.length;
                    lastRequestId = buffer.getInt();
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                    ut2PipeOutput.writeChunk(fileData);
                }

                @Override
                public void onClose() {
                }
            };
        }
    }

}
