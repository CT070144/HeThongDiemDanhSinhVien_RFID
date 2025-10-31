package com.rfid.attendance.service;

import com.rfid.attendance.dto.TokenIntrospectDTO;
import com.rfid.attendance.util.JwtUtil;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TokenIntrospectService {
    @Autowired
    private JwtUtil jwtUtil;

    public TokenIntrospectDTO introspect(String token) {

        boolean valid = jwtUtil.validateToken(token);
        if(valid){
            return TokenIntrospectDTO.builder()
                    .valid(valid)
                    .username(jwtUtil.extractUsername(token))
                    .build();
        }
        return TokenIntrospectDTO.builder()
                .valid(valid)
                .build();
    }
}