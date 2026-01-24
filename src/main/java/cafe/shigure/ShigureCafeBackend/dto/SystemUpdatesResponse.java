package cafe.shigure.ShigureCafeBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemUpdatesResponse {
    private Long noticeLastUpdated;
    private Long userLastUpdated;
    private Long auditLastUpdated;
}
