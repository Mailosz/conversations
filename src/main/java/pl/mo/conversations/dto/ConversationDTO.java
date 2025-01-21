package pl.mo.conversations.dto;

import java.util.UUID;

import lombok.Data;
import pl.mo.conversations.jpa.ConversationEntity;

@Data
public class ConversationDTO {
    final UUID id;
    final String name;

    public ConversationDTO(ConversationEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
    }
}
