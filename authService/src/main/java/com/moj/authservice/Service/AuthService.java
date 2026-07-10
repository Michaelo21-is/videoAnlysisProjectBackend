package com.moj.authservice.Service;

import com.moj.authservice.Configuration.RabbitMqConfig;
import com.moj.authservice.Dto.SignInDto;
import com.moj.authservice.Dto.SignUpDto;
import com.moj.authservice.Entity.TwoFactor;
import com.moj.authservice.Entity.Users;
import com.moj.authservice.Enums.Role;
import com.moj.authservice.Repository.TwoFactorRepository;
import com.moj.authservice.Repository.UsersRepository;
import com.moj.authservice.Response.AuthResponse;
import com.moj.authservice.Response.TwoFactorResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {
    private final UsersRepository usersRepository;
    private final TwoFactorRepository twoFactorRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    public AuthService(UsersRepository usersRepository, PasswordEncoder passwordEncoder, TwoFactorRepository twoFactorRepository, RabbitTemplate rabbitTemplate) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorRepository = twoFactorRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public AuthResponse signUp(SignUpDto signUpDto){
        if (signUpDto.getPassword().length() < 6){
            return AuthResponse.builder()
                    .message("password must be at least 6 characters")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        if(usersRepository.existsByEmail(signUpDto.getEmail())){
            return AuthResponse.builder()
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
        setTwoFactor(user.getId());

        return AuthResponse.builder()
                .message("user created successfully")
                .status(HttpStatus.CREATED)
                .build();
    }



    public AuthResponse signIn(SignInDto signInDto){
        if (signInDto.getPassword().length() < 6){
            return AuthResponse.builder()
                    .message("password must be at least 6 characters")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        String encodedPassword = passwordEncoder.encode(signInDto.getPassword());
        if (usersRepository.existsByEmailAndPassword(signInDto.getEmail(), encodedPassword)){
            return AuthResponse.builder()
                    .message("login successful")
                    .status(HttpStatus.OK)
                    .build();
        }
        return AuthResponse.builder()
                .message("invalid email or password")
                .status(HttpStatus.BAD_REQUEST)
                .build();
    }

    @Transactional
    public AuthResponse verifyTwoFactor(Integer twoFactorCode, UUID userId){
        TwoFactor twoFactor = twoFactorRepository.findByUsersId(userId).
                orElseThrow(()-> new RuntimeException("cannot find the user with this id"));
        if (twoFactor.getTwoFactorCode().equals(twoFactorCode)){
            twoFactorRepository.delete(twoFactor);
            return AuthResponse.builder()
                    .message("two factor code is correct")
                    .status(HttpStatus.OK)
                    .build();
        }
        return AuthResponse.builder()
                .message("two factor code is incorrect")
                .status(HttpStatus.BAD_REQUEST)
                .build();

    }


    ///
        /// private functions
    ///
    private void setTwoFactor(UUID userId){
        Users user = (Users) usersRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("something went wrong with passing the user id check in table if user created before"));
        Integer twoFactorCode = (int) (Math.random() * 900000) + 100000;
        TwoFactor twoFactor = TwoFactor.builder()
                .twoFactorCode(twoFactorCode)
                .ExpirationDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .users(user)
                .build();
        twoFactorRepository.save(twoFactor);
        TwoFactorResponse twoFactorResponse = TwoFactorResponse.builder()
                .verificationCode(twoFactorCode)
                .email(user.getEmail())
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.Exchange,RabbitMqConfig.ROUTING_KEY, twoFactorResponse);
    }

}
