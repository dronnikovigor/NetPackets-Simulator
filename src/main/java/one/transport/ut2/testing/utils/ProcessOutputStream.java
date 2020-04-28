package one.transport.ut2.testing.utils;

import java.io.*;
import java.nio.file.Path;


public class ProcessOutputStream extends Thread {
    InputStream is;
    Path path;

    public ProcessOutputStream(InputStream is, Path path) {
        this.is = is;
        this.path = path;
    }

    @Override
    public void run() {
        try (
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(isr);
            FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream))) {
            while (!isInterrupted()) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    bufferedWriter.write(line + "\n");
                    bufferedWriter.flush();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
