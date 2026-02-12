package cafe.shigure.ShigureCafeBackend.service;

import cafe.shigure.ShigureCafeBackend.model.SystemLog;
import cafe.shigure.ShigureCafeBackend.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogService {
    private final SystemLogRepository systemLogRepository;

    public Page<SystemLog> getLogs(String level, String source, String search, Pageable pageable) {
        return systemLogRepository.findByFilters(level, source, search, pageable);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String level, String source, String content, Long timestamp) {
        SystemLog log = SystemLog.builder()
                .timestamp(timestamp != null ? timestamp : Instant.now().toEpochMilli())
                .level(level)
                .source(source != null ? source : "UNKNOWN")
                .content(content)
                .build();
        systemLogRepository.save(log);
    }

    public void logInternal(String level, String module, String content) {
        String formattedContent = (module != null && !module.isEmpty()) 
                ? String.format("[%s] %s", module, content) 
                : content;
        log(level, "ShigureCafeBackend", formattedContent, null);
    }

    public void info(String module, String content) {
        logInternal("INFO", module, content);
    }

    public void warn(String module, String content) {
        logInternal("WARN", module, content);
    }

    public void error(String module, String content) {
        logInternal("ERROR", module, content);
    }
}
