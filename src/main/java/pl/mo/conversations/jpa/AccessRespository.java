package pl.mo.conversations.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRespository extends JpaRepository<UserConversationAccess, UUID> {
    List<UserConversationAccess> findAllByUserId(UUID userId);
    Optional<UserConversationAccess> findByUserIdAndConversationId(UUID userId, String conversationId);
}
