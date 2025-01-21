package pl.mo.conversations.jpa;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "user_data")
@Data
public class UserData {
    @Id
    UUID id;

    @Column(name = "password")
    String password;
    @Column(name = "username")
    String username;
    @Column(name = "firstname")
    String firstname;
    @Column(name = "lastname")
    String lastname;
    @Column(name = "email")
    String email;
    @Column(name = "phone")
    String phone;
}
