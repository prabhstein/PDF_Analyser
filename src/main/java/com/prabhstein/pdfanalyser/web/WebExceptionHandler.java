package com.prabhstein.pdfanalyser.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(RuntimeException ex, Model model) {
        model.addAttribute("message", cleanMessage(ex));
        return "fragments :: errorBanner";
    }

    private String cleanMessage(RuntimeException ex) {
        if (ex instanceof ResponseStatusException statusException && statusException.getStatusCode() == HttpStatus.NOT_FOUND) {
            return "The requested page was not found.";
        }
        return ex.getMessage() == null ? "Something went wrong." : ex.getMessage();
    }
}
