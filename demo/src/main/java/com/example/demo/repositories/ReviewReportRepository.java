package com.example.demo.repositories;

import com.example.demo.entities.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, String> {

    List<ReviewReport> findByOwnerId(String ownerId);

    List<ReviewReport> findByStartDateAfterAndEndDateBefore(Date start, Date end);
}
