package cafe.shigure.ShigureCafeBackend.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final Map<String, Object> metadata;

    public BusinessException(String code) {
        super(code);
        this.code = code;
        this.metadata = null;
    }

    public BusinessException(String code, Map<String, Object> metadata) {
        super(code);
        this.code = code;
        this.metadata = metadata;
    }
}
