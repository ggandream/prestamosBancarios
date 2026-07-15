package gt.edu.umg.prestamos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica las solicitudes de la API mediante el encabezado {@code X-API-Key}. */
public final class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] expectedApiKey;

    /**
     * Crea el filtro con la clave configurada externamente.
     *
     * @param apiKey clave esperada, obtenida de una variable de entorno
     */
    public ApiKeyAuthenticationFilter(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "APP_API_KEY es obligatoria cuando APP_SECURITY_ENABLED=true");
        }
        this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return !(requestUri.equals("/api") || requestUri.startsWith("/api/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (!isValid(providedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"API key invalida o ausente\"}");
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "api-key", null, List.of()));
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private boolean isValid(String providedApiKey) {
        return providedApiKey != null && MessageDigest.isEqual(
                expectedApiKey, providedApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
