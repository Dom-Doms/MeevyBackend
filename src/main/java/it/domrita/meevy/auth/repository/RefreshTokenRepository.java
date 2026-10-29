
package it.domrita.meevy.auth.repository;

import it.domrita.meevy.auth.model.RefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/*
 * Repository delle sessioni di autenticazione.
 *
 * La ricerca con lock serve a evitare che due richieste
 * simultanee possano rinnovare lo stesso refresh token.
 */
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshToken token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
