package com.qhomebaseapp.repository.registerregistration;

import com.qhomebaseapp.model.RegisterServiceRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class RegisterRegistrationRepoImpl implements RegisterRegistrationRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public RegisterServiceRequest save(RegisterServiceRequest request) {
        if (request.getId() == null) {
            em.persist(request);
            return request;
        } else {
            return em.merge(request);
        }
    }

    @Override
    public List<RegisterServiceRequest> findByUserId(Long userId) {
        return em.createQuery("SELECT r FROM RegisterServiceRequest r WHERE r.userId = :userId", RegisterServiceRequest.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
