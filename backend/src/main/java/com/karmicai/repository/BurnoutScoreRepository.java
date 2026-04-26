package com.karmicai.repository;

import com.karmicai.model.BurnoutScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BurnoutScoreRepository extends JpaRepository<BurnoutScore, Long> {

    BurnoutScore findTopByUserIdOrderByScoreDateDesc(Long userId);

    List<BurnoutScore> findByUserIdAndScoreDateBetween(Long userId, LocalDate start, LocalDate end);

    List<BurnoutScore> findTop3ByUserIdOrderByScoreDateDesc(Long userId);

    /**
     * Aggregate query: returns all scores for a department within a date range.
     * Joins via userId → user table to match institution + department.
     * No individual PII is exposed in results — caller aggregates.
     */
    @Query("""
        SELECT bs FROM BurnoutScore bs
        WHERE bs.scoreDate >= :since
          AND bs.userId IN (
            SELECT u.id FROM User u
            WHERE u.institution = :institution
              AND u.department   = :department
          )
        ORDER BY bs.scoreDate DESC
    """)
    List<BurnoutScore> findByInstitutionAndDeptSince(
        @Param("institution") String institution,
        @Param("department")  String department,
        @Param("since")       LocalDate since
    );
}
