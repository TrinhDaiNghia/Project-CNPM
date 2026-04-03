package com.example.demo.repositories;

import com.example.demo.entities.Discuss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussRepository extends JpaRepository<Discuss, String> {
}

