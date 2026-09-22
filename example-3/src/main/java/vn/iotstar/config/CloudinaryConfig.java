package vn.iotstar.config;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
@Configuration
public class CloudinaryConfig {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);
    @Bean
    Cloudinary cloudinary(
    @Value("${cloudinary.cloud-name}") String cloudName,
    @Value("${cloudinary.api-key}") String apiKey,
    @Value("${cloudinary.api-secret}") String apiSecret,
    ConfigurableEnvironment environment) {
        check("CLOUDINARY_CLOUD_NAME", cloudName);
        check("CLOUDINARY_API_KEY", apiKey);
        check("CLOUDINARY_API_SECRET", apiSecret);
        boolean dotenvLoaded = java.util.stream.StreamSupport.stream(
            environment.getPropertySources().spliterator(), false)
            .anyMatch(source -> source.getName().contains(".env"));
        log.info("Cloudinary configuration: dotenvLoaded={}, requiredVariablesPresent=true", dotenvLoaded);
        return new Cloudinary(Map.of(
        "cloud_name", cloudName,
        "api_key", apiKey,
        "api_secret", apiSecret,
        "secure", true,
        "timeout", 30
        ));
    }

    private void check(String name, String value) {
        if (value == null || value.isBlank() || value.contains("${") || value.startsWith("your_")
                || !value.equals(value.strip()) || value.startsWith("\"") || value.startsWith("'")) {
            throw new IllegalStateException("Kiểm tra biến cấu hình " + name + ": thiếu hoặc sai định dạng Java properties.");
        }
    }
}
