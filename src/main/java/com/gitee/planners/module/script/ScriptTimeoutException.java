package com.gitee.planners.module.script;

public class ScriptTimeoutException extends RuntimeException {

    public ScriptTimeoutException(String message) {
        super(message);
    }

    public ScriptTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
