package rs.sep.parking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.sep.parking.exceptions.DuplicateFoundException;
import rs.sep.parking.dto.RegisterRequest;
import rs.sep.parking.dto.UserResponse;
import rs.sep.parking.model.AppUser;
import rs.sep.parking.model.enums.UserRole;
import rs.sep.parking.repositories.AppUserRepository;
import rs.sep.parking.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static rs.sep.parking.common.constants.ErrorMessages.EMAIL_TAKEN;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    AppUserRepository users;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, passwordEncoder);
    }

    @Test
    void registersDriverWithHashedPassword() {
        given(users.existsByEmailIgnoreCase("ana@example.com")).willReturn(false);
        given(users.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.register(
                new RegisterRequest(" Ana@Example.com ", "Ana A", "lozinka12345"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(saved.capture());
        AppUser user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getFullName()).isEqualTo("Ana A");
        assertThat(user.getRole()).isEqualTo(UserRole.DRIVER);
        assertThat(user.getNotifyBeforeMin()).isEqualTo(5);
        assertThat(user.getPasswordHash()).startsWith("$2").isNotEqualTo("lozinka12345");
        assertThat(passwordEncoder.matches("lozinka12345", user.getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.fullName()).isEqualTo("Ana A");
    }

    @Test
    void existingEmailIsConflict() {
        given(users.existsByEmailIgnoreCase("ana@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("ANA@example.com", "Ana A", "lozinka12345")))
                .isInstanceOf(DuplicateFoundException.class)
                .hasMessage(EMAIL_TAKEN);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void concurrentRegistrationHittingUniqueIndexIsConflict() {
        given(users.existsByEmailIgnoreCase("ana@example.com")).willReturn(false);
        given(users.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("uq email"));

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("ana@example.com", "Ana A", "lozinka12345")))
                .isInstanceOf(DuplicateFoundException.class)
                .hasMessage(EMAIL_TAKEN);
    }
}
