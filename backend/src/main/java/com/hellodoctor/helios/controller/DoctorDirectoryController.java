package com.hellodoctor.helios.controller;

import com.hellodoctor.helios.dto.UserResponse;
import com.hellodoctor.helios.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only doctor directory available to any authenticated user so patients can pick a doctor
 * when booking. Only non-sensitive fields are exposed via {@link UserResponse}.
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorDirectoryController {

    private final UserService userService;

    public DoctorDirectoryController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> doctors() {
        return userService.findDoctors().stream().map(UserResponse::from).toList();
    }
}
