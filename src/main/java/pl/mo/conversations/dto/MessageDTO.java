package pl.mo.conversations.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class MessageDTO {
    String userKey;
    UUID messageId;
    String data;
    Long timestamp;
}
