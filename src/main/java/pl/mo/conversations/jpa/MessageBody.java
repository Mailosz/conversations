package pl.mo.conversations.jpa;

import java.time.Instant;

import lombok.Data;

@Data
public class MessageBody {
    String userKey;
    String data;
    Long timestamp;
}
