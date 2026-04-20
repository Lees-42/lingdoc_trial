package com.ruoyi.lingdoc.common.exception;

import com.ruoyi.common.core.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * LingDoc模块全局异常处理器
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.ruoyi.lingdoc")
public class LingDocExceptionHandler {

    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return AjaxResult.error(message);
    }

    /**
     * 参数校验异常（@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return AjaxResult.error(message);
    }

    /**
     * 权限异常
     */
    @ExceptionHandler(SecurityException.class)
    public AjaxResult handleSecurityException(SecurityException e) {
        log.warn("权限异常: {}", e.getMessage());
        return AjaxResult.error(403, e.getMessage());
    }

    /**
     * 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问拒绝: {}", e.getMessage());
        return AjaxResult.error(403, "无权访问该资源");
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return AjaxResult.error("系统繁忙，请稍后重试");
    }
}
