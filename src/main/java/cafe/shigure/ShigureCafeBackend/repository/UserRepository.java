package cafe.shigure.ShigureCafeBackend.repository;

import cafe.shigure.ShigureCafeBackend.model.User;
import cafe.shigure.ShigureCafeBackend.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByStatus(UserStatus status);

    List<User> findByStatusAndMinecraftUuidIsNotNullAndMinecraftUsernameIsNotNull(UserStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT u.avatarUrl FROM User u WHERE u.avatarUrl IS NOT NULL")
    List<String> findAllAvatarUrls();
}