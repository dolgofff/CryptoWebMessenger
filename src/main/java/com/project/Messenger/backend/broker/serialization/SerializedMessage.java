package com.project.Messenger.backend.broker.serialization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerializedMessage {
    private long id;
    private long roomId;
    private long senderId;
    private long recipientId;
    private byte[] content;
    private String messageIdentity;
    private LocalDateTime timestamp;
}
