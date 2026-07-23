package com.moj.authservice.Component;

import com.moj.authservice.Response.SignInResponse;
import com.moj.authservice.Service.LoginAttemptService;
import com.moj.authservice.Util.ExtractIpFromClient;
import com.moj.authservice.Util.FormatBlockTime;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
// Spring Boot 4 ships Jackson 3, so ObjectMapper lives under tools.jackson, not com.fasterxml.
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;


@Component
public class LoginFailureHandler implements AuthenticationFailureHandler, AuthenticationEntryPoint {

    /** Same message for every invalid-credential case, so the endpoint cannot enumerate accounts. */
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final LoginAttemptService loginAttemptService;
    // Boot's auto-configured mapper, so the body is serialized exactly like a controller response.
    private final ObjectMapper objectMapper;

    public LoginFailureHandler(LoginAttemptService loginAttemptService, ObjectMapper objectMapper) {
        this.loginAttemptService = loginAttemptService;
        this.objectMapper = objectMapper;
    }




    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        onAuthenticationFailure(request, response, authException);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        // Any other AuthenticationException means "no credentials on a protected endpoint",
        // not a wrong password — counting those would let unrelated 401s block the IP.
        if (!(exception instanceof BadCredentialsException)) {
            write(response, SignInResponse.builder()
                    .message("Unauthorized")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build());
            return;
        }

        String ip = ExtractIpFromClient.extractClientIp(request);

        Optional<Duration> blockDuration = loginAttemptService.trackFailedAttempt(ip);

        if (blockDuration.isPresent()) {
            writeBlocked(response, blockDuration.get());
            return;
        }

        write(response, SignInResponse.builder()
                .message(INVALID_CREDENTIALS_MESSAGE)
                .status(HttpStatus.UNAUTHORIZED)
                .build());
    }

    /** The attempt that tripped the limit already reports how long the block lasts. */
    private void writeBlocked(HttpServletResponse response, Duration blockDuration) throws IOException {
        String formattedBlockTime = FormatBlockTime.formatBlockTime(blockDuration);

        response.setHeader("Retry-After", Long.toString(Math.max(blockDuration.getSeconds(), 1)));

        write(response, SignInResponse.builder()
                .message("Too many login attempts. Try again in " + formattedBlockTime)
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .build());
    }

    /*
     * Written straight to the response instead of sendError(): sendError() starts a container
     * ERROR dispatch and replaces the body with Spring Boot's error page, which would both change
     * the response shape and leak the reason.
     *
     * The real HTTP status is taken from the body's own status field, so the two can never drift.
     */
    private void write(HttpServletResponse response, SignInResponse body) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(body.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
