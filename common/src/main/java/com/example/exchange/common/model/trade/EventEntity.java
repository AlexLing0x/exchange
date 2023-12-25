package com.example.exchange.common.model.trade;

import com.example.exchange.common.model.support.EntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "UNI_PREV_ID", columnNames = { "previousId" }))
public class EventEntity implements EntitySupport {
    /**
            * Primary key: assigned.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Keep previous id. The previous id of first event is 0.
     */
    @Column(nullable = false, updatable = false)
    public long previousId;

    /**
     * JSON-encoded event data.
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_10000)
    public String data;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "EventEntity [sequenceId=" + sequenceId + ", previousId=" + previousId + ", data=" + data
                + ", createdAt=" + createdAt + "]";
    }
}
