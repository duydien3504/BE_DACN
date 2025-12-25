package com.example.DACN.config;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.entity.Role;
import com.example.DACN.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            // Create default roles if they don't exist
            createRoleIfNotExists(RoleConstants.ADMIN);
            createRoleIfNotExists(RoleConstants.CUSTOMER);
            createRoleIfNotExists(RoleConstants.SELLER);
        };
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByRoleName(roleName).isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleName);
            roleRepository.save(role);
            log.info("✅ Created role: {}", roleName);
        } else {
            log.info("ℹ️  Role already exists: {}", roleName);
        }
    }
}
