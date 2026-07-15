package gt.edu.umg.prestamos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthenticationFilterTest {

    private static final String API_KEY = "clave-de-prueba-segura";

    @Test
    void permiteSolicitudApiConClaveValida() throws ServletException, IOException {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prestamos");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rechazaSolicitudApiSinClave() throws ServletException, IOException {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prestamos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("API key invalida o ausente");
    }

    @Test
    void dejaPasarRutasPublicasSinClave() throws ServletException, IOException {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void fallaRapidoSiSeguridadActivaNoTieneClave() {
        assertThatThrownBy(() -> new ApiKeyAuthenticationFilter(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_API_KEY");
    }
}
