package cafe.shigure.ShigureCafeBackened.repository;

import cafe.shigure.ShigureCafeBackened.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 自动根据方法名生成 SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
