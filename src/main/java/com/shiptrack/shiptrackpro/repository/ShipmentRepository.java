package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    List<Shipment> findByCreatedBy_Id(Long userId);

    List<Shipment> findByAssignedOperator_Id(Long userId);

    List<Shipment> findByStatusIn(Collection<String> statuses);

    long countByCreatedBy_IdAndStatus(Long userId, String status);

    long countByStatus(String status);

    @Query("select s.status, count(s) from Shipment s group by s.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
            select s from Shipment s
            where s.createdBy.id = :userId
              and s.status = :status
            order by s.createdAt desc
            """)
    List<Shipment> findByCreatedByIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            String status
    );
}
