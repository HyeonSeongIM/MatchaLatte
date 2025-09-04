package project.matchalatte.storage.db.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String username;

    public UserEntity() {
    }

    public UserEntity(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
