package rs.sep.parking.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import rs.sep.parking.exceptions.DuplicateFoundException;
import rs.sep.parking.config.SecurityConfig;
import rs.sep.parking.dto.RegisterRequest;
import rs.sep.parking.dto.UserResponse;
import rs.sep.parking.service.AuthService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static rs.sep.parking.common.constants.ErrorMessages.EMAIL_TAKEN;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService registrationService;

    @Test
    void registersDriverWithNormalizedEmail() throws Exception {
        given(registrationService.register(any()))
                .willReturn(new UserResponse(UUID.randomUUID(), "ana@example.com", "Ana A"));

        post("/api/auth/register", """
                {"email":" Ana@Example.com ","fullName":"Ana A","password":"lozinka12345"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(registrationService).register(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("ana@example.com");
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        given(registrationService.register(any()))
                .willThrow(new DuplicateFoundException(EMAIL_TAKEN));

        post("/api/auth/register", """
                {"email":"ANA@example.com","fullName":"Ana A","password":"lozinka12345"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(EMAIL_TAKEN));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "kratka1",                  // too short
            "samoslovaaaaaa",           // no digit
            "123456789012",             // no letter
            "abcdefghij1234567890abcdefghij1234567890abcdefghij1234567890abcdefghij123" // 73 chars
    })
    void weakPasswordIsBadRequest(String password) throws Exception {
        post("/api/auth/register", """
                {"email":"ana@example.com","fullName":"Ana A","password":"%s"}""".formatted(password))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());

        verify(registrationService, never()).register(any());
    }

    @Test
    void passwordOverBcryptByteLimitIsBadRequest() throws Exception {
        String password = "š".repeat(40) + "a1"; // 42 characters, 82 UTF-8 bytes

        post("/api/auth/register", """
                {"email":"ana@example.com","fullName":"Ana A","password":"%s"}""".formatted(password))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any());
    }

    @Test
    void invalidEmailIsBadRequest() throws Exception {
        post("/api/auth/register", """
                {"email":"not-an-email","fullName":"Ana A","password":"lozinka12345"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    private ResultActions post(String path, String json) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
