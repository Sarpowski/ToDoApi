package com.poly.taskapi.auth;


import com.poly.taskapi.common.security.JwtPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.issuer}")
  private String issuer;

  @Value("${app.jwt.ttl}")
  private Duration ttl;

  private SecretKey key;

  @PostConstruct
  void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(UUID userId, String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setSubject(userId.toString())
        .claim("username", username)
        .setIssuer(issuer)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(ttl)))
        .signWith(key)
        .compact();
  }

  public JwtPrincipal parseToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key)
        .requireIssuer(issuer)
        .build()
        .parseClaimsJws(token)
        .getBody();

    UUID userId = UUID.fromString(claims.getSubject());
    String username = claims.get("username", String.class);
    return new JwtPrincipal(userId, username);
  }

  public boolean isValid(String token) {
    try {
      parseToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

}
