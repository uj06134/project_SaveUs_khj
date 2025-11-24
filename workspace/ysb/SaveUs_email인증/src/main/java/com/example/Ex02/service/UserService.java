package com.example.Ex02.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.example.Ex02.dto.EmailVerificationStatusDto;
import com.example.Ex02.dto.EmailVerificationTokenDto;
import com.example.Ex02.dto.UserJoinDto;
import com.example.Ex02.mapper.EmailVerificationTokenMapper;
import com.example.Ex02.mapper.UserMapper;
import groovyjarjarantlr4.v4.codegen.model.decl.TokenDecl;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailVerificationTokenMapper tokenMapper;

    @Autowired
    private MailService mailService;

    private final int TOKEN_EXPIRE_MINUTES = 30;

    public void sendJoinVerificationMail(String email, HttpSession session) {
        String token = makeToken();

        session.setAttribute("joinToken", token);
        session.setAttribute("joinEmail", email);
        session.setAttribute("joinExpireTime", System.currentTimeMillis() + (TOKEN_EXPIRE_MINUTES * 60 * 1000));

        mailService.sendVerificationCode(email, token);
    }

    // 회원가입용 인증번호 검증
    public boolean verifyJoinCode(String email, String inputCode, HttpSession session) {
        String sessionCode = (String) session.getAttribute("joinToken");
        String sessionEmail = (String) session.getAttribute("joinEmail");
        Long expireTime = (Long) session.getAttribute("joinExpireTime");

        if (sessionCode == null || sessionEmail == null || expireTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime > expireTime) {
            return false;
        }

        if (sessionEmail.equals(email) && sessionCode.equals(inputCode)) {
            session.setAttribute("verifiedEmail", email);
            return true;
        }

        return false;
    }


    @Transactional
    public void deleteUserAll(Long userId) {

        // 1. 자식 테이블 순서대로 삭제
        userMapper.deleteUserGoals(userId);
        userMapper.deleteUserHealthScores(userId);
        userMapper.deleteUserMeals(userId);
        tokenMapper.deleteTokensByUserId(userId);

        // 2. 마지막에 USERS 삭제
        userMapper.deleteUser(userId);
    }

    @Transactional
    public void SendVerification(Long userId) {

        UserJoinDto user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        if ("Y".equalsIgnoreCase(user.getEmailVerified())) {
            return;
        }


        EmailVerificationTokenDto tokenDto = insertToken(userId);
        mailService.sendVerificationEmail(user.getEmail(), tokenDto.getToken());
    }

    @Transactional
    public boolean verifyEmailByToken(String token) {

        EmailVerificationTokenDto validToken = tokenMapper.findValidToken(token);
        if (validToken == null) {
            return false;
        }

        tokenMapper.deleteTokensByUserId(validToken.getUserId());
        userMapper.updateEmailVerified(validToken.getUserId());

        return true;
    }

    @Transactional
    public EmailVerificationStatusDto getVerificationStatusAndResendIfExpired(Long userId) {

        EmailVerificationStatusDto status = new EmailVerificationStatusDto();

        EmailVerificationTokenDto latestToken = tokenMapper.findLatestByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        if (latestToken == null) {
            SendVerification(userId);
            status.setResent(true);
            status.setRemainSeconds(TOKEN_EXPIRE_MINUTES * 60L);
            return status;
        }

        LocalDateTime expiresAt = latestToken.getExpiresAt();

        if (expiresAt.isBefore(now)) {
            SendVerification(userId);
            status.setResent(true);
            status.setRemainSeconds(TOKEN_EXPIRE_MINUTES * 60L);
            return status;
        }

        long remainSeconds = Duration.between(now, expiresAt).getSeconds();
        if (remainSeconds < 0) remainSeconds = 0;

        status.setResent(false);
        status.setRemainSeconds(remainSeconds);
        return status;
    }

    @Transactional
    public void sendPasswordResetToken(UserJoinDto user) {
        Long userId = user.getUserId();
        EmailVerificationTokenDto tokenDto = insertToken(userId);

        mailService.sendPasswordResetEmail(user.getEmail(), tokenDto.getToken());
    }

    @Transactional
    public Long consumePasswordResetToken(String token) {

        EmailVerificationTokenDto validToken = tokenMapper.findValidToken(token);
        if (validToken == null) {
            return null;
        }

        tokenMapper.deleteTokensByUserId(validToken.getUserId());
        return validToken.getUserId();
    }

    private EmailVerificationTokenDto insertToken(Long userId) {
        tokenMapper.deleteTokensByUserId(userId);

        String token = makeToken();
        LocalDateTime now = LocalDateTime.now();

        EmailVerificationTokenDto tokenDto = new EmailVerificationTokenDto();
        tokenDto.setUserId(userId);
        tokenDto.setToken(token);
        tokenDto.setCreatedAt(Timestamp.valueOf(now).toLocalDateTime());
        tokenDto.setExpiresAt(Timestamp.valueOf(now.plusMinutes(TOKEN_EXPIRE_MINUTES)).toLocalDateTime());

        tokenMapper.insertToken(tokenDto);

        return tokenDto;
    }

    private String makeToken() {
        SecureRandom random = new SecureRandom();
        char[] str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        int size = 12;

        return NanoIdUtils.randomNanoId(random, str, size);
    }
}
