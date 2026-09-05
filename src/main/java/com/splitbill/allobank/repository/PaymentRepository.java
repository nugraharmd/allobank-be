package com.splitbill.allobank.repository;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import com.splitbill.allobank.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByGroup(Group group);
}
