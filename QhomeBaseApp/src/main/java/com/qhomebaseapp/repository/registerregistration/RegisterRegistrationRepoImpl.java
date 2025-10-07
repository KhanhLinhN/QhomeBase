package com.qhomebaseapp.repository.registerregistration;

import com.qhomebaseapp.model.RegisterServiceRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class RegisterRegistrationRepoImpl {

    @PersistenceContext
    private EntityManager em;

    public RegisterServiceRequest save(RegisterServiceRequest request) {
        if (request.getId() == null) {
            em.persist(request);
            return request;
        } else {
            return em.merge(request);
        }
    }

    public List<RegisterServiceRequest> findByUserId(Long userId) {
        return em.createQuery("SELECT r FROM RegisterServiceRequest r WHERE r.user.id = :userId", RegisterServiceRequest.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
