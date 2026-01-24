package cafe.shigure.ShigureCafeBackend.repository;

import cafe.shigure.ShigureCafeBackend.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    boolean existsByToken(String token);
    void deleteByExpirationDateBefore(long now);
}
