package com.hellodoctor.helios.controller;

import com.hellodoctor.helios.dto.UserResponse;
import com.hellodoctor.helios.model.UserStatus;
import com.hellodoctor.helios.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin user management. Access is restricted to ADMIN at the security-filter level
 * ({@code /api/users/**} requires ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.getById(id));
    }

    @PutMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return UserResponse.from(userService.setStatus(id, status));
    }
}
