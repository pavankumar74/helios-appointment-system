package com.hellodoctor.helios.security;

import com.hellodoctor.helios.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts the {@link User} entity to Spring Security while carrying the user id and role
 * for authorization decisions in the service layer.
 */
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final com.hellodoctor.helios.model.Role role;
    private final boolean active;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.active = user.getStatus() == com.hellodoctor.helios.model.UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public com.hellodoctor.helios.model.Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
