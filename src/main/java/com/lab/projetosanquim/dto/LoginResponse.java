package com.lab.projetosanquim.dto;

import com.lab.projetosanquim.entity.Role;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm, Role role) {
}
