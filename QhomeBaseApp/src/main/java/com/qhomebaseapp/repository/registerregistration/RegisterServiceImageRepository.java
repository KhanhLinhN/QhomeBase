package com.qhomebaseapp.repository.registerregistration;

import com.qhomebaseapp.model.RegisterServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterServiceImageRepository extends JpaRepository<RegisterServiceImage, Long> {
}
