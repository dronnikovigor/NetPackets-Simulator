package one.transport.ut2.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationProperties {
    public final static Properties applicationProps = new Properties();

    public static void init() throws IOException {
        InputStream inputStream = ApplicationProperties.class
                .getClassLoader().getResourceAsStream("application.properties");

        applicationProps.load(inputStream);
    }
}
