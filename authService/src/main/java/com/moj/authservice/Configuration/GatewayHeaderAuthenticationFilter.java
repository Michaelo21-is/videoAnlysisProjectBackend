package com.moj.authservice.Configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Pre-authentication filter that trusts the identity headers set by the API Gateway.
 *
 * <p>The gateway's {@code JwtAuthenticationFilter} is the only component that validates JWTs.
 * After a successful validation it strips any client-supplied {@code X-USER-*} headers and
 * re-populates them from the token's claims before forwarding the request here:</p>
 * <ul>
 *     <li>ACCESS token  — {@code X-USER-ID}, {@code X-USER-EMAIL} and {@code X-USER-ROLE}
 *     are forwarded, so the user is granted {@code ROLE_<role>}.</li>
 *     <li>TEMPORARY (2FA) token — only {@code X-USER-ID} and {@code X-USER-EMAIL} are
 *     forwarded (deliberately no role), so the user is granted {@link #TEMP_AUTHORITY} only.</li>
 *     <li>No headers — the request stays unauthenticated and the authorization rules in
 *     {@link SecurityConfiguration} decide (public endpoints such as sign-in/sign-up).</li>
 * </ul>
 *
 * <p>This is only safe as long as the auth service is reachable exclusively through the
 * gateway — a caller that can hit this service directly can forge these headers.</p>
 */
@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";
    private static final String USER_ROLE_HEADER = "X-USER-ROLE";

    /** Mirrors the gateway's TEMP_AUTH authority carried by a 2FA temp token. */
    static final String TEMP_AUTHORITY = "TEMP_AUTH";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (userId != null || email != null) {
            // A role header means the gateway validated a full ACCESS token; its absence
            // (while identity headers are present) means a TEMPORARY 2FA token.
            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of(new SimpleGrantedAuthority(TEMP_AUTHORITY));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
