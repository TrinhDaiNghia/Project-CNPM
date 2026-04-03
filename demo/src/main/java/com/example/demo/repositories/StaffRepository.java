package com.example.demo.repositories;

import com.example.demo.entities.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    @Modifying
    @Query(value = "INSERT INTO staffs (id) VALUES (:id)", nativeQuery = true)
    int insertStaffProfile(@Param("id") String id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);


    @Query("SELECT s FROM Staff s " +
            "WHERE (:keyword IS NULL OR " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(s.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Staff> searchStaff(@Param("keyword") String keyword,
                            Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n " +
            "WHERE n.sender.id = :staffId OR n.receiver.id = :staffId")
    boolean existsRelatedRecords(@Param("staffId") String staffId);
}

