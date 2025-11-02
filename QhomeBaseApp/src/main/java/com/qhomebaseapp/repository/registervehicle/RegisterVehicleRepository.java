package com.qhomebaseapp.repository.registervehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.qhomebaseapp.model.RegisterVehicleRequest;
import java.util.List;

@Repository
public interface RegisterVehicleRepository extends JpaRepository<RegisterVehicleRequest, Long> {
    List<RegisterVehicleRequest> findByUser_Id(Long userId);
    Page<RegisterVehicleRequest> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

