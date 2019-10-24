package one.transport.ut2.testing.stand;

import one.transport.ut2.UT2PeerCtx;
import one.transport.ut2.UT2Pipe2Handler;
import one.transport.ut2.UT2PipeInput;
import one.transport.ut2.UT2PipeOutput;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

abstract class AbstractFileSendingTestStand extends AbstractUT2TestStand {
    private final Random rnd = new Random();
    private volatile byte[] fileData;
    final Object syncObj = new Object();
    private Path file;

    final Path generateFileBySize(Path fileDir, int sizeInKb) throws IOException {
        fileData = new byte[sizeInKb * 1024];
        rnd.nextBytes(fileData);

        file = fileDir.resolve(sizeInKb + ".test");
        Files.createFile(file);
        FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
        fileOutputStream.write(fileData);
        fileOutputStream.close();
        return file;
    }

    final void deleteGeneratedFile() throws IOException {
        Files.deleteIfExists(file);
    }

    class ServerHandler implements UT2Pipe2Handler {
        volatile int lastRequestId;
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
