package com.localfood.backend.admin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final String totpSecret;
    private final AdminRole role;
    private final UUID branchId;
    private final boolean active;

    public AdminUserDetails(UUID id, String username, String password, String totpSecret, AdminRole role, UUID branchId, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.totpSecret = totpSecret;
        this.role = role;
        this.branchId = branchId;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public AdminRole getRole() {
        return role;
    }

    public UUID getBranchId() {
        return branchId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == AdminRole.DELIVERY_STAFF) {
            return List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_STAFF"));
        }
        return List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
