package com.italyplanner;

import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String dummyHash = passwords.encode("unused-password-placeholder");
    private final EntityManager entityManager;
    private final UserRepository users;

    public AccountService(EntityManager entityManager, UserRepository users) {
        this.entityManager = entityManager;
        this.users = users;
    }

    @Transactional
    public void register(String email, String password) {
        entityManager.persist(new PlannerUser(email, passwords.encode(password)));
    }

    public boolean authenticate(String email, String password) {
        var user = users.findById(email);
        String hash = user.map(PlannerUser::passwordHash).orElse(dummyHash);
        return passwords.matches(password, hash) && user.isPresent();
    }
}
