package com.qhomebaseapp.repository.registervehicle;

import com.qhomebaseapp.model.RegisterVehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterVehicleImageRepository extends JpaRepository<RegisterVehicleImage, Long> {
}

