package pl.mo.conversations.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import pl.mo.conversations.dto.ConversationDTO;

@Entity(name = "conversations")
@Data
public class ConversationEntity {
    @Id
    UUID id;
    String name;
    Instant createdOn;
    Instant modifiedOn;

    public ConversationEntity(ConversationDTO dto) {
        this.id = dto.getId();
        this.name = dto.getName();
    }
}
