package com.rrss.backend.service;

import com.rrss.backend.dto.ConfirmationTokenDto;
import com.rrss.backend.dto.CreateTokenRequest;
import com.rrss.backend.exception.custom.OtpTokenNotFoundException;
import com.rrss.backend.exception.custom.TokenExpiredException;
import com.rrss.backend.exception.custom.UserAlreadyExistsException;
import com.rrss.backend.exception.custom.WrongOtpException;
import com.rrss.backend.model.ConfirmationToken;
import com.rrss.backend.repository.ConfirmationRepository;
import com.rrss.backend.repository.UserRepository;
import com.rrss.backend.util.EmailUtil;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;


@Service
public class ConfirmationService {
    private final ConfirmationRepository repository;
    private final EmailUtil emailUtil;
    private final UserRepository userRepository;

    @Value("${constants.otp-token-duration}")
    int otpTokenDuration;

    public ConfirmationService(ConfirmationRepository repository, EmailUtil emailUtil, UserRepository userRepository) {
        this.repository = repository;
        this.emailUtil = emailUtil;
        this.userRepository = userRepository;
    }

    private String generateOtp() {
        Random random = new Random();
        int randomNumber = random.nextInt(99999);
        StringBuilder output = new StringBuilder(Integer.toString(randomNumber));

        while (output.length() < 5) {
            output.insert(0, "0");
        }

        return output.toString();
    }

    public ConfirmationTokenDto createToken(CreateTokenRequest createTokenRequest) {

        if(userRepository.existsByEmail(createTokenRequest.email())) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }
        // TODO BU SIGNUP ISI BENCEM SORUNLU ONA BIRDAHA BAK DEGISTIREBILIRIZ. BURADAKI SORUN KULLANICI BUTUN BILGILERINI GIRDIKTEN SONRA EMAIL GIRIYOR.

        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(otpTokenDuration);

        ConfirmationToken confirmationToken = repository.findByEmail(createTokenRequest.email())
                .map(existingToken -> new ConfirmationToken(
                        existingToken.getId(),
                        generateOtp(),
                        existingToken.getEmail(),
                        expirationTime))
                .orElseGet(() -> new ConfirmationToken(
                        generateOtp(),
                        createTokenRequest.email(),
                        expirationTime));

        try {
            emailUtil.sendOtpEmail(confirmationToken.getEmail(), confirmationToken.getOtp());
        } catch (MessagingException e) {
            throw new RuntimeException("Unable to send otp please try again");
        }

        return ConfirmationTokenDto.convert(confirmationToken);
    }

    public void checkOtp(String otp, String email) {

        ConfirmationToken token =  repository
                .findByEmail(email)
                .orElseThrow(
                        () -> new OtpTokenNotFoundException("Otp token with this email: " + email + "not found.")
                );

        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            createToken(new CreateTokenRequest(email));
            throw new TokenExpiredException("Token is expired new token is send to your mail.");
        } else if(!token.getOtp().equals(otp)) {
            throw new WrongOtpException("You have written wrong otp.");
        }
    }
}