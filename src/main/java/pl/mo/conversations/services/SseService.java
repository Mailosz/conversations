package pl.mo.conversations.services;



import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.mo.conversations.dto.MessageDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final Map<String,List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    public void addEmitter(String conversationId, SseEmitter emitter) {

        List<SseEmitter> emitters = emittersMap.get(conversationId);
        if (emitters == null) {
            synchronized (this) {
                if (!emittersMap.containsKey(conversationId)) {
                    emitters = new CopyOnWriteArrayList<>();
                    emittersMap.put(conversationId, emitters);
                }
            }
        }

        List<SseEmitter> effectivelyFinalEmitters = emitters;
        emitters.add(emitter);
        emitter.onCompletion(() -> effectivelyFinalEmitters.remove(emitter));
        emitter.onTimeout(() -> effectivelyFinalEmitters.remove(emitter));
    }

    public void sendEvents(String conversationId, UUID key, String message) {

        List<SseEmitter> emitters = emittersMap.get(conversationId);

        try {
            var dto = new ObjectMapper().readValue(message, MessageDTO.class);
            dto.setMessageId(key);

            if (emitters != null) {
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(dto);
                    } catch (IOException e) {
                        emitter.complete();
                        emitters.remove(emitter);
                    }
                }
            }
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }
}