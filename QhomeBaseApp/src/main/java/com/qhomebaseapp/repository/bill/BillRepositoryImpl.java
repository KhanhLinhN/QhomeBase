package com.qhomebaseapp.repository.bill;

import com.qhomebaseapp.model.Bill;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class BillRepositoryImpl implements BillRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Bill> searchBills(Long userId, String month, String billType, String status) {
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b WHERE b.user.id = :userId");
        List<String> conditions = new ArrayList<>();

        if (month != null && !month.isEmpty()) {
            jpql.append(" AND b.billingMonth = :month");
        }
        if (billType != null && !billType.isEmpty()) {
            jpql.append(" AND b.billType = :billType");
        }
        if (status != null && !status.isEmpty()) {
            jpql.append(" AND b.status = :status");
        }

        jpql.append(" ORDER BY b.billingMonth DESC");

        TypedQuery<Bill> query = entityManager.createQuery(jpql.toString(), Bill.class);
        query.setParameter("userId", userId);

        if (month != null && !month.isEmpty()) {
            query.setParameter("month", month);
        }
        if (billType != null && !billType.isEmpty()) {
            query.setParameter("billType", billType);
        }
        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }
}
