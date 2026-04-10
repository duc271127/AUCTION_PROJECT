package com.team.backend.repository;

import com.team.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> { }
