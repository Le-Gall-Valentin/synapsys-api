package com.synapsys.api.infrastructure.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative rate limiting for controller endpoints.
 * IP-based limiting is always active; username-based is opt-in.
 *
 * Example:
 * <pre>
 *   {@code @PostMapping("/login")}
 *   {@code @RateLimiting(byUsername = true)}
 *   public ResponseEntity<?> login(@RequestBody LoginRequest req) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiting {

    /** Maximum attempts per IP within the IP window. */
    int ipMax() default 10;

    /** Rolling window in seconds for IP-based limiting. */
    int ipWindowSeconds() default 60;

    /** Whether to also apply per-username limiting. */
    boolean byUsername() default false;

    /** Maximum attempts per username within the username window. */
    int usernameMax() default 20;

    /** Rolling window in seconds for username-based limiting. */
    int usernameWindowSeconds() default 300;

    /**
     * Name of the field (record accessor or getter) on the first {@code @RequestBody}
     * argument that holds the username string.
     */
    String usernameField() default "username";
}