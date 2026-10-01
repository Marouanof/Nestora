package com.userservice.userservice.config;

import com.userservice.userservice.entity.Role;
import com.userservice.userservice.enums.RoleName;
import com.userservice.userservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (RoleName name : RoleName.values()) {
            roleRepository.findByName(name).orElseGet(() ->
                    roleRepository.save(Role.builder().name(name).build()));
        }
    }
}
