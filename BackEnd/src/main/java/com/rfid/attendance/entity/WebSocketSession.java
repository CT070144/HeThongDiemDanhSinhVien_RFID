package com.rfid.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.time.Instant;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "websocketsession")
public class WebSocketSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String socketSessionId;
    String userId;
    Instant createdAt;
}
