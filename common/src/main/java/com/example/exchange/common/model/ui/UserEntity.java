package com.example.exchange.common.model.ui;

import com.example.exchange.common.enums.UserType;
import com.example.exchange.common.model.support.EntitySupport;
import jakarta.persistence.*;


@Entity
@Table(name = "users")
public class UserEntity implements EntitySupport {
    /**
     * Primary key: auto-increment long.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public UserType type;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "UserEntity [id=" + id + ", type=" + type + ", createdAt=" + createdAt + "]";
    }
}
