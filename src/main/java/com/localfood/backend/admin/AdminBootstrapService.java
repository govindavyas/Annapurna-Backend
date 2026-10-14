package com.localfood.backend.admin;

import com.localfood.backend.config.AppSecurityProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private final AdminAccountRepository repository;
    private final AppSecurityProperties props;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(AdminAccountRepository repository, AppSecurityProperties props, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.props = props;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.countByRole(AdminRole.SUPER_ADMIN) > 0) {
            return;
        }

        AdminAccount account = new AdminAccount();
        account.setUsername(props.username().trim());
        account.setPasswordHash(passwordEncoder.encode(props.password()));
        account.setTotpSecret(props.totpSecret().trim());
        account.setRole(AdminRole.SUPER_ADMIN);
        account.setActive(true);
        repository.save(account);
    }
}
