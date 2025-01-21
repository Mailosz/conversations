package pl.mo.conversations.dto;

import lombok.Data;
import pl.mo.conversations.jpa.AccessLevel;
import pl.mo.conversations.jpa.UserData;

@Data
public class AccessInfo {
    final AccessLevel level;
    final UserData user;
}
