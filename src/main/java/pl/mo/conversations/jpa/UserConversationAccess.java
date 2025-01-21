package pl.mo.conversations.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "user_conversation_access")
@Data
public class UserConversationAccess {
    @Id
    UUID id;

    @Column(name = "user_id")
    UUID userId;

    @Column(name = "conversation_id")
    String conversationId;

    @Enumerated(EnumType.STRING)
    AccessLevel level;

    @Column(name = "given_on")
    Instant givenOn;
    @Column(name = "revoked_on")
    Instant revokedOn;
}
