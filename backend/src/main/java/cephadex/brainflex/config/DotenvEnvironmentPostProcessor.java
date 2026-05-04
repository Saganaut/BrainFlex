package cephadex.brainflex.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Path file = Path.of(".env");
        if (!Files.exists(file)) {
            file = Path.of("../.env");
        }
        if (!Files.exists(file))
            return;

        Properties props = new Properties();
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                int eq = line.indexOf('=');
                if (eq < 1)
                    continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // Strip surrounding single or double quotes
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                props.setProperty(key, value);
            }
        } catch (IOException e) {
            return;
        }

        // addLast so system env vars and application.properties still take precedence
        environment.getPropertySources().addLast(new PropertiesPropertySource("dotenv", props));
    }
}
