package pl.mo.conversations.dto;

import java.util.List;

import lombok.Data;

@Data
public class ConversationFeedDTO {
    String name;
    String conversationId;
    String userKey;
    String subscriptionCode;
    List<MessageDTO> messages;
}
