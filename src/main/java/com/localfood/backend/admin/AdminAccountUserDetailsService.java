package com.localfood.backend.admin;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminAccountUserDetailsService implements UserDetailsService {

    private final AdminAccountRepository repository;

    public AdminAccountUserDetailsService(AdminAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccount account = repository.findByUsernameIgnoreCase(username)
            .filter(user -> !user.isDeleted() && (user.getBranch() == null || !user.getBranch().isDeleted()))
            .orElseThrow(() -> new UsernameNotFoundException("Admin user not found"));

        return new AdminUserDetails(
            account.getId(),
            account.getUsername(),
            account.getPasswordHash(),
            account.getTotpSecret(),
            account.getRole(),
            account.getBranch() == null ? null : account.getBranch().getId(),
            account.isActive()
        );
    }
}
