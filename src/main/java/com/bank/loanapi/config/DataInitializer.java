package com.bank.loanapi.config;

import com.bank.loanapi.entity.Customer;
import com.bank.loanapi.entity.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Skip if data already exists (using import.sql)
        if (userRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            return;
        }

        // Create admin user
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(User.Role.ADMIN))
                    .build();
            userRepository.save(adminUser);
            log.info("Admin user created with username: admin and password: admin123");
        }

        // Create test customers with users
        if (!userRepository.existsByUsername("john.doe")) {
            User johnUser = User.builder()
                    .username("john.doe")
                    .password(passwordEncoder.encode("password123"))
                    .roles(Set.of(User.Role.CUSTOMER))
                    .build();
            johnUser = userRepository.save(johnUser);

            Customer johnCustomer = Customer.builder()
                    .name("John")
                    .surname("Doe")
                    .creditLimit(new BigDecimal("10000.00"))
                    .usedCreditLimit(BigDecimal.ZERO)
                    .user(johnUser)
                    .build();
            customerRepository.save(johnCustomer);
            log.info("Customer John Doe created with username: john.doe and password: password123");
        }

        if (!userRepository.existsByUsername("jane.smith")) {
            User janeUser = User.builder()
                    .username("jane.smith")
                    .password(passwordEncoder.encode("password123"))
                    .roles(Set.of(User.Role.CUSTOMER))
                    .build();
            janeUser = userRepository.save(janeUser);

            Customer janeCustomer = Customer.builder()
                    .name("Jane")
                    .surname("Smith")
                    .creditLimit(new BigDecimal("15000.00"))
                    .usedCreditLimit(BigDecimal.ZERO)
                    .user(janeUser)
                    .build();
            customerRepository.save(janeCustomer);
            log.info("Customer Jane Smith created with username: jane.smith and password: password123");
        }
    }
}