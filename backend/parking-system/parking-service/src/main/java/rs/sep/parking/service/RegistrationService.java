package rs.sep.parking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.sep.parking.exceptions.DuplicateFoundException;
import rs.sep.parking.dto.RegisterRequest;
import rs.sep.parking.dto.UserResponse;
import rs.sep.parking.model.AppUser;
import rs.sep.parking.model.enums.UserRole;
import rs.sep.parking.repositories.AppUserRepository;

import static rs.sep.parking.common.constants.ErrorMessages.EMAIL_TAKEN;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateFoundException(EMAIL_TAKEN);
        }
        AppUser user = AppUser.builder()
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.DRIVER)
                .build();
        try {
            // Two concurrent requests can both pass the check above; the unique index decides.
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFoundException(EMAIL_TAKEN);
        }
        return UserResponse.from(user);
    }
}
