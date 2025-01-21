package pl.mo.conversations.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.conversations.dto.ConversationDTO;
import pl.mo.conversations.jpa.ConversationEntity;
import pl.mo.conversations.jpa.ConversationRepository;

@Service
public class ConversationService {

    @Autowired
    ConversationRepository conversationRepository;
    

    public ConversationEntity createConversation(ConversationDTO dto) {

        ConversationEntity entity = new ConversationEntity(dto);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCreatedOn(Instant.now());

        return conversationRepository.save(entity);
    }
}
