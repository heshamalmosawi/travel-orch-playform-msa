package com.sayedhesham.travelorch.common.repository.report;

import com.sayedhesham.travelorch.common.entity.report.ManagerReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerReportRepository extends JpaRepository<ManagerReport, Long> {

    long countByManagerId(Long managerId);

    long countByReporterId(Long reporterId);

    boolean existsByManagerIdAndReporterId(Long managerId, Long reporterId);
}
