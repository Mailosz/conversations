package pl.mo.conversations.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "session_data")
@Data
public class SessionEntity {
    @Id
    @Column(name = "session_key")
    String sessionKey;
    @Column(name = "user_id")
    UUID userId;
    @Column(name = "started_on")
    Instant startedOn;
    @Column(name = "last_used")
    Instant lastUsed;
    @Column(name = "active")
    boolean active = true;
}
