package se300.shiftlift;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class for password hashing and verification using BCrypt.
 * Provides secure password handling for user authentication.
 */
final class PasswordUtil {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {}

    /**
     * Hashes a password using BCrypt.
     * If already hashed, returns the hash unchanged.
     * 
     * @param raw the plain text password
     * @return the BCrypt hash of the password
     */
    static String hash(String raw) {
        if (raw == null) return null;
        if (isBcryptHash(raw)) return raw;
        return ENCODER.encode(raw);
    }

    /**
     * Verifies a password against a hash.
     * Supports BCrypt hashes and legacy plaintext (for backward compatibility).
     * 
     * @param raw the plain text password to verify
     * @param hashed the stored hash or password
     * @return true if password matches, false otherwise
     */
    static boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) return false;
        if (isBcryptHash(hashed)) {
            return ENCODER.matches(raw, hashed);
        }
        return raw.equals(hashed);
    }

    /**
     * Checks if a string is a BCrypt hash.
     * 
     * @param value the string to check
     * @return true if the string is a BCrypt hash, false otherwise
     */
    static boolean isBcryptHash(String value) {
        if (value == null) return false;
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
