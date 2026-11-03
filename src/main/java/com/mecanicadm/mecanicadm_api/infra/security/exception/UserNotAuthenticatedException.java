package com.mecanicadm.mecanicadm_api.infra.security.exception;

public class UserNotAuthenticatedException extends SecurityException {
    public UserNotAuthenticatedException() {
        super("error.user.not.authenticated");
    }
}
