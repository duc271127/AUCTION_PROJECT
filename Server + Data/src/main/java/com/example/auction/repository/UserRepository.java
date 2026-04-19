package com.example.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.auction.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
