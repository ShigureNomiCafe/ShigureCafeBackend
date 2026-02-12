package cafe.shigure.ShigureCafeBackend.repository;

import cafe.shigure.ShigureCafeBackend.model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    @Query("SELECT s FROM SystemLog s WHERE " +
           "(:level IS NULL OR s.level = :level) AND " +
           "(:source IS NULL OR s.source = :source) AND " +
           "(:search IS NULL OR LOWER(s.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SystemLog> findByFilters(@Param("level") String level,
                                  @Param("source") String source,
                                  @Param("search") String search,
                                  Pageable pageable);
}
