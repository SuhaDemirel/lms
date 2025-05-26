package com.bank.loanapi.controller;

import com.bank.loanapi.dto.request.LoginRequest;
import com.bank.loanapi.dto.response.JwtAuthenticationResponse;
import com.bank.loanapi.entity.Customer;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.security.CustomUserDetails;
import com.bank.loanapi.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomerRepository customerRepository;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and get JWT token")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Get customer ID if user has CUSTOMER role
        Long customerId = null;
        if (userDetails.getUser().getRoles().stream().anyMatch(role -> role.name().equals("CUSTOMER"))) {
            Customer customer = customerRepository.findByUserId(userDetails.getUser().getId()).orElse(null);
            if (customer != null) {
                customerId = customer.getId();
            }
        }

        JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .username(userDetails.getUsername())
                .roles(userDetails.getUser().getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .customerId(customerId)
                .build();

        return ResponseEntity.ok(response);
    }
}
