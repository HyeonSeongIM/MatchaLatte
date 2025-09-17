package project.matchalatte.core.support.error;

public class ErrorMessage {

    private final String code;

    private final String message;

    private final Object data;

    private final String traceId;

    public ErrorMessage(ErrorType errorType, String traceId) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.traceId = traceId;
        this.data = null;
    }

    public ErrorMessage(ErrorType errorType, Object data, String traceId) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.data = data;
        this.traceId = traceId;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

}
