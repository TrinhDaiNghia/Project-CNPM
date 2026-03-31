package com.example.demo.repositories;

import com.example.demo.entities.RevenueReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RevenueReportRepository extends JpaRepository<RevenueReport, String> {

    List<RevenueReport> findByOwnerId(String ownerId);

    List<RevenueReport> findByStartDateAfterAndEndDateBefore(Date start, Date end);
}
