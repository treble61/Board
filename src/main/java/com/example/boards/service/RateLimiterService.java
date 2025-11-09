package com.example.boards.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Bucket4j Token Bucket algorithm
 *
 * Limits:
 * - Login: 5 attempts per 15 minutes per IP
 * - Signup: 3 attempts per hour per IP
 * - Password Change: 3 attempts per 15 minutes per IP
 * - Email Verification: 10 attempts per hour per IP
 * - Resend Verification: 3 attempts per hour per email
 */
@Service
public class RateLimiterService {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> signupBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordChangeBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailVerificationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> resendVerificationBuckets = new ConcurrentHashMap<>();

    /**
     * Check if login attempt is allowed for this IP
     * Limit: 5 attempts per 15 minutes
     */
    public boolean allowLogin(String identifier) {
        Bucket bucket = loginBuckets.computeIfAbsent(identifier, k -> createLoginBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if signup attempt is allowed for this IP
     * Limit: 3 attempts per hour
     */
    public boolean allowSignup(String identifier) {
        Bucket bucket = signupBuckets.computeIfAbsent(identifier, k -> createSignupBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if password change attempt is allowed for this IP
     * Limit: 3 attempts per 15 minutes
     */
    public boolean allowPasswordChange(String identifier) {
        Bucket bucket = passwordChangeBuckets.computeIfAbsent(identifier, k -> createPasswordChangeBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Create bucket for login attempts: 5 tokens, refill 5 tokens every 15 minutes
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for signup attempts: 3 tokens, refill 3 tokens every hour
     */
    private Bucket createSignupBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for password change attempts: 3 tokens, refill 3 tokens every 15 minutes
     */
    private Bucket createPasswordChangeBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(15)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Check if email verification attempt is allowed for this IP
     * Limit: 10 attempts per hour
     */
    public boolean allowEmailVerification(String identifier) {
        Bucket bucket = emailVerificationBuckets.computeIfAbsent(identifier, k -> createEmailVerificationBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if resend verification email is allowed for this email
     * Limit: 3 attempts per hour
     */
    public boolean allowResendVerification(String identifier) {
        Bucket bucket = resendVerificationBuckets.computeIfAbsent(identifier, k -> createResendVerificationBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Create bucket for email verification attempts: 10 tokens, refill 10 tokens every hour
     */
    private Bucket createEmailVerificationBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for resend verification attempts: 3 tokens, refill 3 tokens every hour
     */
    private Bucket createResendVerificationBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Reset rate limit for a specific identifier and endpoint
     * Useful for testing or manual intervention
     */
    public void resetLimit(String identifier, String endpoint) {
        switch (endpoint.toLowerCase()) {
            case "login":
                loginBuckets.remove(identifier);
                break;
            case "signup":
                signupBuckets.remove(identifier);
                break;
            case "password":
                passwordChangeBuckets.remove(identifier);
                break;
            case "email-verification":
                emailVerificationBuckets.remove(identifier);
                break;
            case "resend-verification":
                resendVerificationBuckets.remove(identifier);
                break;
        }
    }

    /**
     * Clear all rate limit buckets
     * Use with caution - primarily for testing
     */
    public void clearAll() {
        loginBuckets.clear();
        signupBuckets.clear();
        passwordChangeBuckets.clear();
        emailVerificationBuckets.clear();
        resendVerificationBuckets.clear();
    }
}
