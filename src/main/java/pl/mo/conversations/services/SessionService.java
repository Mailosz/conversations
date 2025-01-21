package pl.mo.conversations.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.mo.conversations.dto.AccessInfo;
import pl.mo.conversations.dto.SessionDTO;
import pl.mo.conversations.jpa.AccessLevel;
import pl.mo.conversations.jpa.AccessRespository;
import pl.mo.conversations.jpa.SessionEntity;
import pl.mo.conversations.jpa.SessionRepository;
import pl.mo.conversations.jpa.UserConversationAccess;
import pl.mo.conversations.jpa.UserData;
import pl.mo.conversations.jpa.UserRepository;

@Service
public class SessionService {

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccessRespository accessRespository;
    
    public String getSession(UserData user) {
        String sessionKey = UUID.randomUUID().toString();

        SessionEntity session = new SessionEntity();
        session.setUserId(user.getId());
        session.setStartedOn(Instant.now());
        session.setLastUsed(Instant.now());
        session.setSessionKey(sessionKey);

        sessionRepository.save(session);

        return sessionKey;
    }

    public boolean test(String sessionKey) {
        var session =  sessionRepository.findById(sessionKey);
        return session.isPresent();
    }

    public Optional<SessionEntity> checkUserSession(String sessionKey) {
        var session = sessionRepository.findById(sessionKey);
        return session;
    }

    public AccessInfo checkUserSession(String sessionKey, String conversationId) {

        var session = sessionRepository.findById(sessionKey);
    
        if (session.isEmpty()) { 
            return new AccessInfo(AccessLevel.NO_ACCESS, null);
        }

        var user = userRepository.findById(session.get().getUserId());
        if (user.isEmpty()) { 
            return new AccessInfo(AccessLevel.NO_ACCESS, null);
        }

        var access = accessRespository.findByUserIdAndConversationId(user.get().getId(), conversationId);
        if (access.isEmpty()) {
            return new AccessInfo(AccessLevel.NO_ACCESS, null);
        }

        return new AccessInfo(access.get().getLevel(), user.get()); 
    }

    public List<UserConversationAccess> getUserAccess(String sessionKey) {

        var session = sessionRepository.findById(sessionKey);
    
        if (session.isEmpty()) { 
            return new ArrayList();
        }

        var user = userRepository.findById(session.get().getUserId());
        if (user.isEmpty()) { 
            return new ArrayList();
        }

        var conversations = accessRespository.findAllByUserId(user.get().getId());

        return conversations;
    }
}
