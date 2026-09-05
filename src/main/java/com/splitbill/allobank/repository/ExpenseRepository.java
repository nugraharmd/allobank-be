package com.splitbill.allobank.repository;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroup(Group group);
}
