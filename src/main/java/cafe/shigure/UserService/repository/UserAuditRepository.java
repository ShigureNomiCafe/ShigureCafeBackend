package cafe.shigure.UserService.repository;

import cafe.shigure.UserService.model.UserAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
    Optional<UserAudit> findByAuditCode(String auditCode);
    Optional<UserAudit> findByUserId(Long userId);
}
