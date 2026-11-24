package com.yarlinson.gestortareas.auth;

public record TokenResponse(String token, String tipo, long expiraEnSegundos) {

    public static TokenResponse bearer(String token, long expiraEnSegundos) {
        return new TokenResponse(token, "Bearer", expiraEnSegundos);
    }
}
