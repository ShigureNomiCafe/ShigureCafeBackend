package cafe.shigure.ShigureCafeBackend.repository;

import cafe.shigure.ShigureCafeBackend.model.UserAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
    Optional<UserAudit> findByAuditCode(String auditCode);
    Optional<UserAudit> findByUserId(Long userId);
}
