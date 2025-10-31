package com.rfid.attendance.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TokenIntrospectDTO {
    boolean valid;
    String username;
}
