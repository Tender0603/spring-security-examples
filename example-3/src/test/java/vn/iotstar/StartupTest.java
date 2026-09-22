package vn.iotstar;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.iotstar.service.CloudinaryService;
import vn.iotstar.service.EmailService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = "spring.datasource.url=jdbc:h2:mem:startup;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class StartupTest {
    @LocalServerPort int port;
    @MockitoBean EmailService email;
    @MockitoBean CloudinaryService cloud;

    @Test void embeddedTomcatServesDashboardAndLoginOverHttp() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : new String[]{"/", "/login"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains(path.equals("/") ? "IOTSTAR SHOP" : "Đăng nhập");
            }
        }
    }
}
