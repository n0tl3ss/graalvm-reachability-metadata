/*
 * Copyright and related rights waived via CC0
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package io_jsonwebtoken.jjwt_gson;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.CompressionCodecs;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Jjwt_gsonTest {
    @Test
    void testSignedJWTs() {
        SecretKey firstKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String secretString = Encoders.BASE64.encode(firstKey.getEncoded());
        SecretKey secondKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretString));
        assertThat(Jwts.parserBuilder().setSigningKey(firstKey).build().parseClaimsJws(
                Jwts.builder().setSubject("Joe").signWith(firstKey).compact()
        ).getBody().getSubject()).isEqualTo("Joe");
        assertThat(Jwts.parserBuilder().setSigningKey(secondKey).build().parseClaimsJws(
                Jwts.builder().setSubject("Joe").signWith(secondKey).compact()
        ).getBody().getSubject()).isEqualTo("Joe");
    }

    @Test
    void testCreatingAJWS() {
        Date firstDate = new Date();
        Date secondDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
        String uuidString = UUID.randomUUID().toString();
        SecretKey firstKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String firstCompactJws = Jwts.builder()
                .setSubject("Joe")
                .setHeaderParam("kid", "myKeyId")
                .setIssuer("Aaron")
                .setAudience("Abel")
                .setExpiration(secondDate)
                .setNotBefore(firstDate)
                .setIssuedAt(firstDate)
                .setId(uuidString)
                .claim("exampleClaim", "Adam")
                .signWith(firstKey, SignatureAlgorithm.HS256)
                .compressWith(CompressionCodecs.GZIP)
                .compact();
        JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder().setAllowedClockSkewSeconds(3 * 60).setSigningKey(firstKey);
        assertThat(jwtParserBuilder.build().parseClaimsJws(firstCompactJws).getBody().getSubject()).isEqualTo("Joe");
        assertDoesNotThrow(() -> jwtParserBuilder.requireSubject("Joe").build().parseClaimsJws(firstCompactJws));
        assertDoesNotThrow(() -> jwtParserBuilder.requireIssuer("Aaron").build().parseClaimsJws(firstCompactJws));
        assertDoesNotThrow(() -> jwtParserBuilder.requireAudience("Abel").build().parseClaimsJws(firstCompactJws));
        // This is an error caused by GSON's own parsing
        assertThrows(IncorrectClaimException.class, () -> jwtParserBuilder.requireExpiration(secondDate).build().parseClaimsJws(firstCompactJws));
        assertThrows(IncorrectClaimException.class, () -> jwtParserBuilder.requireNotBefore(firstDate).build().parseClaimsJws(firstCompactJws));
        assertThrows(IncorrectClaimException.class, () -> jwtParserBuilder.requireIssuedAt(firstDate).build().parseClaimsJws(firstCompactJws));
        assertThrows(IncorrectClaimException.class, () -> jwtParserBuilder.requireId(uuidString).build().parseClaimsJws(firstCompactJws));
        assertThrows(IncorrectClaimException.class, () -> jwtParserBuilder.require("exampleClaim", "Adam").build().parseClaimsJws(firstCompactJws));
    }

    @Test
    void testCompression() {
        SecretKey firstKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        assertThat(Jwts.parserBuilder().setSigningKey(firstKey).build().parseClaimsJws(
                Jwts.builder().setSubject("Joe").signWith(firstKey).compressWith(CompressionCodecs.DEFLATE).compact()
        ).getBody().getSubject()).isEqualTo("Joe");
        assertThat(Jwts.parserBuilder().setSigningKey(firstKey).build().parseClaimsJws(
                Jwts.builder().setSubject("Joe").signWith(firstKey).compressWith(CompressionCodecs.GZIP).compact()
        ).getBody().getSubject()).isEqualTo("Joe");
    }

    @Test
    void testSignatureAlgorithms() {
        Stream.of(SignatureAlgorithm.HS256, SignatureAlgorithm.HS384, SignatureAlgorithm.HS512)
                .map(Keys::secretKeyFor)
                .forEach(secretKey -> assertThat(Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(
                        Jwts.builder().setSubject("Joe").signWith(secretKey).compact()
                ).getBody().getSubject()).isEqualTo("Joe"));
        Stream.of(SignatureAlgorithm.ES256, SignatureAlgorithm.ES384, SignatureAlgorithm.ES512,
                        SignatureAlgorithm.RS256, SignatureAlgorithm.RS384, SignatureAlgorithm.RS512,
                        SignatureAlgorithm.PS256, SignatureAlgorithm.PS384, SignatureAlgorithm.PS512)
                .map(Keys::keyPairFor)
                .forEach(keyPair -> assertThat(Jwts.parserBuilder().setSigningKey(keyPair.getPublic()).build().parseClaimsJws(
                        Jwts.builder().setSubject("Joe").signWith(keyPair.getPrivate()).compact()
                ).getBody().getSubject()).isEqualTo("Joe"));
    }
}
