package pl.mo.conversations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.mo.conversations.dto.ConversationDTO;
import pl.mo.conversations.dto.ConversationFeedDTO;
import pl.mo.conversations.dto.LoginDTO;
import pl.mo.conversations.dto.MessageDTO;
import pl.mo.conversations.dto.SessionDTO;
import pl.mo.conversations.jpa.AccessLevel;
import pl.mo.conversations.jpa.ConversationEntity;
import pl.mo.conversations.jpa.MessageBody;
import pl.mo.conversations.jpa.UserRepository;
import pl.mo.conversations.services.ConversationService;
import pl.mo.conversations.services.SessionService;
import pl.mo.conversations.services.SseService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@CrossOrigin(allowCredentials = "true", origins = "http://localhost:8080")
public class ConversationController {

    @Autowired
    KafkaTemplate<UUID, String> kafkaTemplate;

    @Autowired
    ConsumerFactory<UUID, String> consumerFactory;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionService sessionService;

    @Autowired
    ConversationService conversationService;
    
    @PostMapping("/session/key") 
    public String getSessionKey(@RequestBody LoginDTO login) {

        var user = userRepository.findByUsername(login.getUsername());

        if (user.isPresent() && (user.get().getPassword() == null || user.get().getPassword().equals(login.getPassword()))) {
            var sessionKey = sessionService.getSession(user.get());
            return sessionKey;
        } else {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }
    }

    @PostMapping("/session/test") 
    public void getSessionKey(@RequestBody String sessionKey) {

        if (!sessionService.test(sessionKey)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

    }

    @GetMapping("/conversations")
    public List<String> getConversations(@RequestHeader("Session-key") String sessionKey) throws JsonProcessingException {
        var conversations = sessionService.getUserAccess(sessionKey);
        return conversations.stream().map((uca) -> uca.getConversationId()).toList();
    }

    @PostMapping("/conversations")
    public ConversationDTO addConversation(@RequestHeader("Session-key") String sessionKey, @RequestBody ConversationDTO dto) throws JsonProcessingException {

        var session = sessionService.checkUserSession(sessionKey);
        if (session.isEmpty()) throw new ResponseStatusException(HttpStatusCode.valueOf(403));

        var created = conversationService.createConversation(dto);

        return new ConversationDTO(created);
    }
    
    @PostMapping("/conversation/{conversationId}")
    public MessageDTO sendMessage(@RequestHeader("Session-key") String sessionKey, @PathVariable String conversationId, @RequestBody MessageBody message) throws JsonProcessingException {

        var access = sessionService.checkUserSession(sessionKey, conversationId);
        if (access.getLevel() == AccessLevel.NO_ACCESS) throw new ResponseStatusException(HttpStatusCode.valueOf(403));

        if (message.getTimestamp() == null) {
            message.setTimestamp(Instant.now().toEpochMilli());
        }
        message.setUserKey(access.getUser().getId().toString());
        
        ObjectMapper mapper = new ObjectMapper();
        var messageString = mapper.writeValueAsString(message);
        
        var key = UUID.randomUUID();
        kafkaTemplate.send(conversationId, 0, key, messageString);

        MessageDTO dto = new MessageDTO();
        dto.setUserKey(message.getUserKey());
        dto.setTimestamp(message.getTimestamp());
        dto.setData(message.getData());
        dto.setMessageId(key);
        return dto;
    }

    Map<UUID, String> oneTimeSubscriptionCodes = new HashMap<UUID, String>();

    @GetMapping(value="/conversation/{conversationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ConversationFeedDTO getMessages(@RequestHeader("Session-key") String sessionKey, @PathVariable String conversationId) {

        var access = sessionService.checkUserSession(sessionKey, conversationId);

        if (access.getLevel() == AccessLevel.NO_ACCESS) throw new ResponseStatusException(HttpStatusCode.valueOf(403));

        var consumer = consumerFactory.createConsumer("groupId","conversationConsumer");
        var TopicPartitionList = List.of(new TopicPartition(conversationId, 0));
        // consumer.subscribe(List.of(conversationId));
        consumer.assign(TopicPartitionList);
        consumer.seekToBeginning(TopicPartitionList);
        var records = consumer.poll(Duration.ofMillis(5000));


        List<MessageDTO> messages = new ArrayList<MessageDTO>();
        ObjectMapper mapper = new ObjectMapper();
        records.forEach((record) -> {
            try {
                MessageBody messageBody = mapper.readValue(record.value(), MessageBody.class);

                MessageDTO dto = new MessageDTO();
                dto.setUserKey(messageBody.getUserKey());
                dto.setMessageId(record.key());
                dto.setTimestamp(messageBody.getTimestamp());
                dto.setData(messageBody.getData());

                messages.add(dto);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        UUID oneTimeCode = UUID.randomUUID();
        oneTimeSubscriptionCodes.put(oneTimeCode, conversationId);
        
        ConversationFeedDTO feed = new ConversationFeedDTO();
        feed.setSubscriptionCode(oneTimeCode.toString());
        feed.setUserKey(access.getUser().getId().toString());
        feed.setMessages(messages);
        return feed;
    }

    @PutMapping("/conversation/{conversationId}")
    public List<MessageBody> updateMessage(@PathVariable String conversationId) {
            
        
        return null;
    }


    @Autowired
    SseService sseService;

    @GetMapping(path = "/subscribe/{subscriptionCode}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID subscriptionCode) {

        var conversationId = oneTimeSubscriptionCodes.remove(subscriptionCode);

        if (conversationId != null) {
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
            sseService.addEmitter(conversationId, emitter);
            return emitter;
        } else {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

    }


    // @GetMapping(value = "/updates/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // public SseEmitter streamSseMvc(@PathVariable String conversationId) {
    //     SseEmitter emitter = new SseEmitter();
    //     ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
    //     sseMvcExecutor.execute(() -> {
    //         try {
    //             for (int i = 0; true; i++) {
    //                 SseEventBuilder event = SseEmitter.event()
    //                 .data("SSE MVC - " + LocalTime.now().toString())
    //                 .id(String.valueOf(i))
    //                 .name("sse event - mvc");
    //                 emitter.send(event);
    //                 Thread.sleep(1000);
    //             }
    //         } catch (Exception ex) {
    //             emitter.completeWithError(ex);
    //         }
    //     });
    //     return emitter;
    // }
}
