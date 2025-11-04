import br.com.guiarq.GuiaRQApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = GuiaRQApplication.class)
@ActiveProfiles("test")
class GuiaRQApplicationTests {

    @Test
    void contextLoads() {
        // Apenas para garantir que o contexto inicia
    }
}
