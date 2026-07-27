package com.hellodoctor.helios.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.hellodoctor.helios.model.Role;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.model.UserStatus;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtMzItYnl0ZXMtbG9uZyE=";

    private SecurityUser sampleUser() {
        User user = User.builder()
                .name("Ada Lovelace")
                .email("ada@example.com")
                .password("hashed")
                .role(Role.PATIENT)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(42L);
        return new SecurityUser(user);
    }

    @Test
    void generatesAndValidatesToken() {
        JwtService jwtService = new JwtService(SECRET, 3600000L);
        String token = jwtService.generateToken(sampleUser());

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("ada@example.com");
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 3600000L);
        String token = jwtService.generateToken(sampleUser());

        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThat(jwtService.isValid(tampered)).isFalse();
        assertThat(jwtService.extractUsername(tampered)).isNull();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1000L); // already expired
        String token = jwtService.generateToken(sampleUser());

        assertThat(jwtService.isValid(token)).isFalse();
    }
}
