package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.AdminAuthResponse;
import com.tss.springsecurity.dto.AdminLoginRequest;
import com.tss.springsecurity.dto.AdminRegisterRequest;
import com.tss.springsecurity.dto.DashboardStatsResponse;

public interface AdminService {
    
    AdminAuthResponse register(AdminRegisterRequest registerRequest);
    
    AdminAuthResponse login(AdminLoginRequest loginRequest);
    
    DashboardStatsResponse getDashboardStats();
}
