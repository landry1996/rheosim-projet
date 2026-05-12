package com.rheosim.domain.identity.port;

import com.rheosim.domain.identity.model.User;

public interface TokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    String extractEmail(String token);

    boolean validateToken(String token);
}
