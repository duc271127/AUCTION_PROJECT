package com.example.auction.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.auction.entity.User;
import com.example.auction.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAll() {
        return userService.getAllUsers();
    }
}
