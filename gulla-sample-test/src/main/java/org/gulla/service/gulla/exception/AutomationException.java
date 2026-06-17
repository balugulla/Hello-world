package org.gulla.service.gulla.exception;

public class AutomationException extends RuntimeException {
    private final String stage;

    public AutomationException(String stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }
}
