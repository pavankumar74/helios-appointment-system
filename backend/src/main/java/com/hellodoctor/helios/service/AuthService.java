package com.hellodoctor.helios.service;

import com.hellodoctor.helios.dto.AuthResponse;
import com.hellodoctor.helios.dto.LoginRequest;
import com.hellodoctor.helios.dto.RegisterRequest;
import com.hellodoctor.helios.exception.ConflictException;
import com.hellodoctor.helios.model.Role;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.model.UserStatus;
import com.hellodoctor.helios.repository.UserRepository;
import com.hellodoctor.helios.security.JwtService;
import com.hellodoctor.helios.security.SecurityUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }

        // Public self-registration is limited to PATIENT and DOCTOR. ADMIN accounts are
        // provisioned by existing admins only.
        Role requestedRole = request.role() == null ? Role.PATIENT : request.role();
        Role role = (requestedRole == Role.ADMIN) ? Role.PATIENT : requestedRole;

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .status(UserStatus.ACTIVE)
                .specialty(role == Role.DOCTOR ? request.specialty() : null)
                .phone(request.phone())
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(new SecurityUser(user), user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user missing"));
        return buildAuthResponse(principal, user);
    }

    private AuthResponse buildAuthResponse(SecurityUser principal, User user) {
        String token = jwtService.generateToken(principal);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }
}
