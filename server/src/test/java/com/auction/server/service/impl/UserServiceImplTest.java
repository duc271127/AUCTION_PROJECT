package com.auction.server.service.impl;

import com.auction.server.dto.LoginByEmailDto;
import com.auction.server.dto.LoginDto;
import com.auction.server.dto.UserDto;
import com.auction.server.entity.Bidder;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void authenticate_returnsUserWhenSelectedRoleMatchesAccountRole() {
        Bidder bidder = bidder("bidder01", "bidder@example.com", "secret123", "BIDDER");
        LoginDto dto = new LoginDto();
        dto.username = "bidder01";
        dto.password = "secret123";
        dto.role = "BIDDER";

        when(userRepository.findByUsername("bidder01")).thenReturn(Optional.of(bidder));

        UserDto result = userService.authenticate(dto);

        assertEquals("BIDDER", result.role);
        assertEquals("bidder01", result.username);
    }

    @Test
    void authenticate_throwsWhenSelectedRoleDoesNotMatchAccountRole() {
        Bidder bidder = bidder("bidder01", "bidder@example.com", "secret123", "BIDDER");
        LoginDto dto = new LoginDto();
        dto.username = "bidder01";
        dto.password = "secret123";
        dto.role = "ADMIN";

        when(userRepository.findByUsername("bidder01")).thenReturn(Optional.of(bidder));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> userService.authenticate(dto));

        assertEquals("Selected role does not match this account", error.getMessage());
    }

    @Test
    void authenticateByEmail_throwsWhenSelectedRoleDoesNotMatchAccountRole() {
        Bidder bidder = bidder("bidder01", "bidder@example.com", "secret123", "BIDDER");
        LoginByEmailDto dto = new LoginByEmailDto();
        dto.email = "bidder@example.com";
        dto.password = "secret123";
        dto.role = "SELLER";

        when(userRepository.findByEmail("bidder@example.com")).thenReturn(Optional.of(bidder));

        BusinessRuleException error = assertThrows(
                BusinessRuleException.class,
                () -> userService.authenticateByEmail(dto)
        );

        assertEquals("Selected role does not match this account", error.getMessage());
    }

    private Bidder bidder(String username, String email, String rawPassword, String role) {
        Bidder bidder = new Bidder();
        bidder.setUsername(username);
        bidder.setEmail(email);
        bidder.setPasswordHash(passwordEncoder.encode(rawPassword));
        bidder.setRole(role);
        return bidder;
    }
}
