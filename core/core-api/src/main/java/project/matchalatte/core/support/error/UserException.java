package project.matchalatte.core.support.error;

public class UserException extends RuntimeException {

    private final ErrorType errorType;

    private final Object data;


    public UserException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
    }

    public UserException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Object getData() {
        return data;
    }

}
