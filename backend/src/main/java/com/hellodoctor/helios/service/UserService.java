package com.hellodoctor.helios.service;

import com.hellodoctor.helios.exception.ResourceNotFoundException;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.model.UserStatus;
import com.hellodoctor.helios.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> findDoctors() {
        return userRepository.findByRole(com.hellodoctor.helios.model.Role.DOCTOR);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Transactional
    public User setStatus(Long id, UserStatus status) {
        User user = getById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }
}
