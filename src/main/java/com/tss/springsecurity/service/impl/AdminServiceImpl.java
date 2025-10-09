package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.AdminAuthResponse;
import com.tss.springsecurity.dto.AdminLoginRequest;
import com.tss.springsecurity.dto.AdminRegisterRequest;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.service.AdminService;
import com.tss.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AdminAuthResponse register(AdminRegisterRequest registerRequest) {
        // Check if username already exists
        if (adminRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (adminRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new admin
        Admin admin = new Admin();
        admin.setUsername(registerRequest.getUsername());
        admin.setEmail(registerRequest.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

        // Save admin
        Admin savedAdmin = adminRepository.save(admin);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedAdmin.getUsername());

        // Return response
        return new AdminAuthResponse(
                savedAdmin.getAdminId(),
                savedAdmin.getUsername(),
                savedAdmin.getEmail(),
                token
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuthResponse login(AdminLoginRequest loginRequest) {
        // Find admin by username or email
        Admin admin = adminRepository.findByUsernameOrEmail(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(admin.getUsername());

        // Return response
        return new AdminAuthResponse(
                admin.getAdminId(),
                admin.getUsername(),
                admin.getEmail(),
                token
        );
    }
}
