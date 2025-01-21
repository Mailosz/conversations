package pl.mo.conversations.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserData, UUID> {
    
    Optional<UserData> findByUsername(String username);
}
