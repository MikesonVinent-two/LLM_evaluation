package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomPasswordEncoder implements PasswordEncoder {
    
    private final BCryptPasswordEncoder encoder;
    
    public CustomPasswordEncoder() {
        this.encoder = new BCryptPasswordEncoder();
    }
    
    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
} 