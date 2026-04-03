package com.example.demo.repositories;

import com.example.demo.entities.Owner;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, String> {

	@Modifying
	@Query(value = "INSERT INTO owners (id) VALUES (:id)", nativeQuery = true)
	int insertOwnerProfile(@Param("id") String id);
}
