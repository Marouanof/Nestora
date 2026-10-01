package com.userservice.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class IncompleteProfileException extends RuntimeException {
    private final String code;
    private final List<String> missing;
    private final String href;

    public IncompleteProfileException(String message) {
        super(message);
        this.code = "KYC_INCOMPLETE";
        this.missing = List.of();
        this.href = "/profile?missing=KYC";
    }

    public IncompleteProfileException(String message, List<String> missing, String href) {
        super(message);
        this.code = "KYC_INCOMPLETE";
        this.missing = missing != null ? List.copyOf(missing) : List.of();
        this.href = href != null ? href : "/profile?missing=KYC";
    }
}
