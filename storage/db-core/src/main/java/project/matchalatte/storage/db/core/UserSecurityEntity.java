package project.matchalatte.storage.db.core;

import jakarta.persistence.*;

@Entity
@Table(name = "user_security")
public class UserSecurityEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private UserEntity user;

    @Column(nullable = false, unique = true, length = 120)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    public UserSecurityEntity() {
    }

    public UserSecurityEntity(UserEntity user, String username, String password) {
        this.user = user;
        this.username = username;
        this.password = password;
    }

    public Long getId(){
        return id;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}

