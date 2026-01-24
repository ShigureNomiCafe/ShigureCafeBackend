package cafe.shigure.ShigureCafeBackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private Map<String, Object> metadata;

    public static ErrorResponse of(String code) {
        return new ErrorResponse(code, null);
    }

    public static ErrorResponse of(String code, Map<String, Object> metadata) {
        return new ErrorResponse(code, metadata);
    }
}
