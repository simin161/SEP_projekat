package rs.sep.parking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank
        @Pattern(regexp = RegisterRequest.PASSWORD_PATTERN,
                message = "must be 12 to 72 characters long and contain at least one letter and one digit")
        String password) {

    /**72 is BCrypt's input limit. */
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{12,72}$";

    private static final int BCRYPT_MAX_BYTES = 72;

    public RegisterRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        fullName = fullName == null ? null : fullName.strip();
    }

    /** BCrypt rejects inputs over 72 bytes; non-ASCII characters can exceed that within 72 characters. */
    @AssertTrue(message = "password must be at most 72 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", fullName=" + fullName + "]";
    }
}
