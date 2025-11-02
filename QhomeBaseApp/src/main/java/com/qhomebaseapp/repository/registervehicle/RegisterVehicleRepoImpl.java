package com.qhomebaseapp.repository.registervehicle;

import com.qhomebaseapp.model.RegisterVehicleRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class RegisterVehicleRepoImpl {

    @PersistenceContext
    private EntityManager em;

    public RegisterVehicleRequest save(RegisterVehicleRequest request) {
        if (request.getId() == null) {
            em.persist(request);
            return request;
        } else {
            return em.merge(request);
        }
    }

    public List<RegisterVehicleRequest> findByUserId(Long userId) {
        return em.createQuery("SELECT r FROM RegisterVehicleRequest r WHERE r.user.id = :userId", RegisterVehicleRequest.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}

