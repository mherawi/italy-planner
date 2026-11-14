package com.italyplanner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<PlannerUser, String> {
}
