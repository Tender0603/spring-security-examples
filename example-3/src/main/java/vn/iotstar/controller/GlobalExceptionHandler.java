package vn.iotstar.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.iotstar.service.CloudinaryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CloudinaryOperationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String cloudinary(CloudinaryOperationException error, Model model) {
        // The adapter has already logged safe diagnostics; never expose a raw SDK cause.
        model.addAttribute("message", error.getMessage());
        return "error";
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(IllegalArgumentException error, Model model) {
        model.addAttribute("message", error.getMessage());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Model model) {
        model.addAttribute("message", "Dữ liệu bị trùng hoặc đang được sử dụng. Vui lòng kiểm tra lại.");
        return "error";
    }

    @ExceptionHandler(MailException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String mail(Model model) {
        model.addAttribute("message", "Không gửi được email. Hãy kiểm tra cấu hình SMTP và thử lại.");
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String upload(Model model) {
        model.addAttribute("message", "Ảnh vượt quá giới hạn 10 MB.");
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String integration(IllegalStateException error, Model model) {
        // Unknown exception text may include credentials; only log its type.
        log.error("Application operation failed: type={}, message=Operation could not be completed",
            error.getClass().getSimpleName());
        model.addAttribute("message", "Dịch vụ chưa sẵn sàng. Hãy kiểm tra cấu hình email/Cloudinary và thử lại.");
        return "error";
    }
}
