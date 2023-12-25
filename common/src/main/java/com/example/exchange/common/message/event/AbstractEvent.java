package com.example.exchange.common.message.event;

import com.example.exchange.common.message.AbstractMessage;
import org.springframework.lang.Nullable;

public class AbstractEvent extends AbstractMessage {

    /**
     * Message id, set after sequenced.
     */
    public long sequenceId;

    /**
     * Previous message sequence id.
     */
    public long previousId;

    /**
     * Unique id or null if not set.
     */
    @Nullable
    public String uniqueId;
}
