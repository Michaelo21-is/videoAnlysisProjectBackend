package com.moj.authservice.Service;

import com.moj.authservice.Component.LoginFailureHandler;
import com.moj.authservice.Configuration.RabbitMqConfig;
import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Entity.ResetPasswordTicket;
import com.moj.authservice.Entity.TwoFactor;
import com.moj.authservice.Entity.Users;
import com.moj.authservice.Enums.Role;
import com.moj.authservice.Enums.TokenType;
import com.moj.authservice.Enums.TwoFactorType;
import com.moj.authservice.Repository.ResetPasswordTicketRepository;
import com.moj.authservice.Repository.TwoFactorRepository;
import com.moj.authservice.Repository.UsersRepository;
import com.moj.authservice.Response.*;
import com.moj.authservice.Util.ExtractIpFromClient;
import com.moj.authservice.Util.FormatBlockTime;
import com.moj.authservice.Util.GenerateResetToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UsersRepository usersRepository;
    private final TwoFactorRepository twoFactorRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final JwtService jwtService;
    private final ResetPasswordTicketRepository resetPasswordTicketRepository;
    private final LoginAttemptService loginAttemptService;
    private final ResetPasswordAttemptService resetPasswordAttemptService;
    private final HttpServletRequest request;

    public AuthService(UsersRepository usersRepository, PasswordEncoder passwordEncoder, ResetPasswordTicketRepository resetPasswordTicketRepository
            , TwoFactorRepository twoFactorRepository, RabbitTemplate rabbitTemplate, JwtService jwtService,
                       LoginAttemptService loginAttemptService, HttpServletRequest request, ResetPasswordAttemptService resetPasswordAttemptService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorRepository = twoFactorRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.jwtService = jwtService;
        this.resetPasswordTicketRepository = resetPasswordTicketRepository;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
        this.resetPasswordAttemptService = resetPasswordAttemptService;
    }
    @Transactional
    public TempTokenResponse signUp(SignUpDto signUpDto){
        if (signUpDto.getPassword().length() < 6){
            return TempTokenResponse.builder()
                    .message("password must be at least 6 characters")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        if(usersRepository.existsByEmail(signUpDto.getEmail())){
            return TempTokenResponse.builder()
                    .message("email already exists")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        String encodedPassword = passwordEncoder.encode(signUpDto.getPassword());
        Users user = Users
                .builder()
                .fullName(signUpDto.getFullName())
                .email(signUpDto.getEmail())
                .password(encodedPassword)
                .role(Role.USER)
                .createdDate(LocalDate.now())
                .creditSum(0L)
                .build();
        usersRepository.save(user);
        setTwoFactor(user.getId(), TwoFactorType.EMAILVERIFICATION);

        String jwtTemp = jwtService.generateToken(user, TokenType.TEMPORARY);


        return TempTokenResponse.builder()
                .message("user created successfully")
                .tempToken(jwtTemp)
                .status(HttpStatus.CREATED)
                .build();
    }


    @Transactional
    public SignInResponse signIn(SignInDto signInDto){
        String ip = ExtractIpFromClient.extractClientIp(request);

        Optional<Duration> blockTime = loginAttemptService.getRemainingBlockTime(ip);
        if (blockTime.isPresent()) {
            String formattedBlockTime = FormatBlockTime.formatBlockTime(blockTime.get());
            return SignInResponse.builder()
                    .message("Too many login attempts. Try again in " + formattedBlockTime)
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }

        /*
         * if it fails spring boot security is deploying badCredntaials and from there is saving the attempt of user
         */
        if (signInDto.getPassword() == null || signInDto.getPassword().length() < 6){
            throw new BadCredentialsException(LoginFailureHandler.INVALID_CREDENTIALS_MESSAGE);
        }
        Users user = usersRepository.findUserByEmail(signInDto.getEmail())
        .orElse(null);
        if (user == null || !passwordEncoder.matches(signInDto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(LoginFailureHandler.INVALID_CREDENTIALS_MESSAGE);
            }

        // The credentials were correct, so the IP starts again from a clean slate.
        loginAttemptService.clearFailedAttempts(ip);

        if (!user.isUserVerifiedEmail()){
            setTwoFactor(user.getId(), TwoFactorType.EMAILVERIFICATION);
            String tempToken = jwtService.generateToken(user, TokenType.TEMPORARY);
            return SignInResponse.builder()
                    .message("please verify your email")
                    .status(HttpStatus.OK)
                    .requiresEmailVerification(true)
                    .tempToken(tempToken)
                    .build();
        }
        String accessToken = jwtService.generateToken(user, TokenType.ACCESS);
        String refreshToken = jwtService.generateToken(user, TokenType.REFRESH);
        jwtService.saveToken(refreshToken, user);

        return SignInResponse.builder()
                .message("login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(HttpStatus.OK)
                .build();
    }



    @Transactional
    public AccessAndRefreshResponse verifyTwoFactor(Integer twoFactorCode, UUID userId){
        TwoFactor twoFactor = twoFactorRepository.findByUsersId(userId).
                orElseThrow(() -> new IllegalStateException("Two-factor record was not found for user: " + userId));
        if (twoFactor.getTwoFactorCode().equals(twoFactorCode) && twoFactor.getExpirationDate().isAfter(Instant.now())){
            twoFactor.getUsers().setUserVerifiedEmail(true);
            usersRepository.save(twoFactor.getUsers());
            String accessToken = jwtService.generateToken(twoFactor.getUsers(), TokenType.ACCESS);
            String refreshToken = jwtService.generateToken(twoFactor.getUsers(), TokenType.REFRESH);
            jwtService.saveToken(refreshToken, twoFactor.getUsers());
            twoFactorRepository.delete(twoFactor);
            return AccessAndRefreshResponse.builder()
                    .message("two factor code is correct")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .status(HttpStatus.OK)
                    .build();
        }
        return AccessAndRefreshResponse.builder()
                .message("two factor code is incorrect")
                .status(HttpStatus.BAD_REQUEST)
                .build();

    }



    @Transactional
    public void setTwoFactor(UUID userId, TwoFactorType twoFactorType){
        Users user = usersRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("something went wrong with passing the user id check in table if user created before"));
        int twoFactorCode = SECURE_RANDOM.nextInt(900000) + 100000;
        TwoFactor twoFactor = twoFactorRepository.findByUsersId(userId)
                .orElseGet(() -> TwoFactor.builder()
                        .users(user)
                        .build()
                );

        twoFactor.setTwoFactorCode(twoFactorCode);
        twoFactor.setExpirationDate((Instant.now().plus(15, ChronoUnit.MINUTES)));
        twoFactor.setTwoFactorType(twoFactorType);
        twoFactorRepository.save(twoFactor);

        TwoFactorResponse twoFactorResponse = TwoFactorResponse.builder()
                .verificationCode(twoFactorCode)
                .email(user.getEmail())
                .twoFactorType(twoFactorType)
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.AUTH_NOTIFICATION_EXCHANGE,RabbitMqConfig.TWO_FACTOR_ROUTING_KEY, twoFactorResponse);
    }



    @Transactional
    public RegularResponse restPasswordRequest(String email){
        // checking if the user didnt hit the limit of reset password request
        String ip = ExtractIpFromClient.extractClientIp(request);
        Optional<Duration> blockTime = resetPasswordAttemptService.getRemainingBlockTime(ip);
        if (blockTime.isPresent()) {
            String formattedBlockTime = FormatBlockTime.formatBlockTimeForReset(blockTime.get());
            return RegularResponse.builder()
                    .message("Too many reset password attempts. Try again in " + formattedBlockTime)
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }
        Optional<Duration> newBlock = resetPasswordAttemptService.trackResetAttempt(ip);
        if (newBlock.isPresent()) {String formattedBlockTime = FormatBlockTime.formatBlockTimeForReset(newBlock.get());
            return RegularResponse.builder()
                    .message("Too many password reset requests. Try again in " + formattedBlockTime)
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }


        Users user = usersRepository.findUserByEmail(email)
                .orElse(null);
        if (user == null){
            return RegularResponse.builder()
                    .message("No account was found with this email address.")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        String resetToken = GenerateResetToken.generateResetToken();
        // saving in the database
        byte[] hashedToken = GenerateResetToken.hashToken(resetToken);
        ResetPasswordTicket resetPasswordTicket =
                resetPasswordTicketRepository
                        .findByUsersId(user.getId())
                        .orElseGet(() ->
                                ResetPasswordTicket.builder()
                                        .users(user)
                                        .build()
                        );
        resetPasswordTicket.setResetToken(hashedToken);
        resetPasswordTicket.setExpirationDate(Instant.now().plus(15, ChronoUnit.MINUTES));
        resetPasswordTicketRepository.save(resetPasswordTicket);
        // sending it to notification service
        ResetPasswordResponse resetPasswordResponse = ResetPasswordResponse.builder()
                .email(email)
                .resetToken(resetToken)
                .build();

        rabbitTemplate.convertAndSend(RabbitMqConfig.AUTH_NOTIFICATION_EXCHANGE, RabbitMqConfig.PASSWORD_RESET_ROUTING_KEY, resetPasswordResponse);

        return RegularResponse.builder()
                .message("password reset request sent successfully")
                .status(HttpStatus.OK)
                .build();
    }

    public boolean isResetTokenValid(String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return false;
        }
        byte[] hashedToken = GenerateResetToken.hashToken(resetToken);
        return resetPasswordTicketRepository.existsByResetTokenAndExpirationDateAfter(hashedToken, Instant.now());
    }

    @Transactional
    public RegularResponse setNewPassword(String resetToken, String newPassword){
        if (resetToken == null || resetToken.isBlank()) {
            return RegularResponse.builder()
                    .message("reset token is missing")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        byte[] hashResetToken = GenerateResetToken.hashToken(resetToken);
        ResetPasswordTicket resetPasswordTicket = resetPasswordTicketRepository.findByResetToken(hashResetToken)
                .orElseThrow(() ->   new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reset token is invalid or expired"));
        if (newPassword.length() < 6){
            return RegularResponse.builder()
                    .message("password must be at least 6 characters")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        if (!resetPasswordTicket.getExpirationDate().isAfter(Instant.now())){
            return RegularResponse.builder()
                    .message("reset token has expired")
                    .status(HttpStatus.GONE)
                    .build();
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        resetPasswordTicket.getUsers().setPassword(encodedPassword);
        usersRepository.save(resetPasswordTicket.getUsers());
        jwtService.deleteToken(resetPasswordTicket.getUsers().getId());
        resetPasswordTicketRepository.delete(resetPasswordTicket);
        return RegularResponse.builder()
                .message("password reset successful")
                .status(HttpStatus.OK)
                .build();
    }

    public boolean isUserAllowedOnTwoFactorPage(String email, UUID userId) {
        if (email == null || userId == null) {
            return false;
        }
        return usersRepository.existsByIdAndEmail(userId, email);
    }
    @Transactional
    public AccessAndRefreshResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return AccessAndRefreshResponse.builder()
                    .message("refresh token is missing")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        return jwtService.renewAccessToken(refreshToken);
    }
    public void signOut(UUID userId){jwtService.deleteToken(userId);}
}
