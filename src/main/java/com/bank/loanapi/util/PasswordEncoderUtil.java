package com.bank.loanapi.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/**
 * Utility class to generate BCrypt encoded passwords for import.sql
 * Run this class's main method to generate encoded passwords
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate encoded passwords
        System.out.println("=== Password Encoder Utility ===");
        System.out.println("Use these encoded passwords in your import.sql file:\n");

        // Admin password
        String adminPassword = "admin123";
        System.out.println("admin / " + adminPassword);
        System.out.println("Encoded: " + encoder.encode(adminPassword));
        System.out.println();

        // Customer passwords
        String customerPassword = "password123";
        System.out.println("All customers / " + customerPassword);
        System.out.println("Encoded: " + encoder.encode(customerPassword));
        System.out.println();

        // Custom passwords
        System.out.println("=== Generate custom passwords ===");
        String[] customPasswords = {"test123", "secure456", "demo789"};
        for (String password : customPasswords) {
            System.out.println(password + " -> " + encoder.encode(password));
        }

        // Verify passwords
        System.out.println("\n=== Verification ===");
        String encodedAdmin = "$2a$10$YdXBPLPSTtVWsQ.hN0VZMeJ8o6u3yBTBfDY7GXkWQ1IEr7AHxH5bC";
        System.out.println("Verifying admin password: " + encoder.matches(adminPassword, encodedAdmin));

        String encodedCustomer = "$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa";
        System.out.println("Verifying customer password: " + encoder.matches(customerPassword, encodedCustomer));
    }
}