package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final Role role;
    private final Collection<GrantedAuthority> authorities;

    public CustomUserDetails(UUID userId, Role role) {
        this.userId = userId;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    // Pas de mot de passe dans le principal JWT
    @Override
    public String getPassword() {
        return null;
    }

    // Le compte est stateless JWT — is_active est vérifié à la connexion dans LoginHandler
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}