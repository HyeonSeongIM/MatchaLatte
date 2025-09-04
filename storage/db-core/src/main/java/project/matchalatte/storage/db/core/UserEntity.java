package project.matchalatte.storage.db.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    public UserEntity() {
    }

    public UserEntity(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }
}
