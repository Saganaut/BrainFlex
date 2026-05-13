/**
 * Redis-backed HTTP session storage.
 *
 * Before Phase 3 of the auth-hardening plan the servlet container kept session
 * state in process memory ({@code HttpSessionSecurityContextRepository} writing
 * to the Tomcat in-memory map), which meant every backend restart logged
 * everyone out and we could never run more than one app instance. With Spring
 * Session Data Redis enabled here, {@code HttpServletRequest.getSession()}
 * returns a session whose attributes are persisted to the Redis container
 * already running in compose.yaml — sessions survive restarts and scale across
 * instances. The existing security wiring did not need to change: the same
 * {@code HttpSessionSecurityContextRepository} now reads/writes to the
 * Redis-backed session.
 *
 * Gated to !test so the test suite continues to use the servlet container's
 * in-memory session store. HealthControllerTest mocks RedisConnectionFactory
 * and would otherwise fail to deserialize sessions on every request.
 *
 * The cookie attributes match what we want in production: HttpOnly so JS can
 * never read it, SameSite=Lax so OAuth redirects still carry it back from
 * accounts.google.com while CSRF from a different origin is blocked, and the
 * Secure flag auto-detected from the request (false on http://localhost, true
 * once the app is behind HTTPS).
 */
package cephadex.brainflex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@Profile("!test")
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 1209600, // 14 days
        redisNamespace = "brainflex:session")
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("BRAINFLEX_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        // useSecureCookie left unset → auto-detected from request.isSecure().
        // dev (http://localhost) keeps Secure=false; HTTPS deploys get Secure=true.
        return serializer;
    }
}
