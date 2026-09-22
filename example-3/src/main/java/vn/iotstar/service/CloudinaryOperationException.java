package vn.iotstar.service;

import java.util.Locale;

/** Safe diagnostics only: provider responses may contain API keys or signed requests. */
public class CloudinaryOperationException extends IllegalStateException {
    private final String category;
    private final String failureType;

    private CloudinaryOperationException(String category, String failureType, String message) {
        // Deliberately do not retain the raw SDK exception as a cause.
        super(message);
        this.category = category;
        this.failureType = failureType;
    }

    public String getCategory() { return category; }
    public String getFailureType() { return failureType; }

    public static CloudinaryOperationException invalidResponse() {
        return new CloudinaryOperationException("INVALID_RESPONSE", "InvalidCloudinaryResponse",
            "Cloudinary không trả về secure_url/public_id hợp lệ. Ảnh chưa được lưu.");
    }

    public static CloudinaryOperationException from(Exception error) {
        Throwable root = error;
        StringBuilder messages = new StringBuilder();
        // Inspect messages to classify, but never return/log any of their contents.
        for (int depth = 0; root != null && depth < 12; depth++) {
            messages.append(' ').append(root.getMessage());
            if (root.getCause() == null || root.getCause() == root) break;
            root = root.getCause();
        }
        String message = messages.toString().toLowerCase(Locale.ROOT);
        String type = root.getClass().getSimpleName();
        if (message.contains("api_key") || message.contains("api key")) {
            return new CloudinaryOperationException("API_KEY", type,
                "Cloudinary từ chối API key. Kiểm tra CLOUDINARY_API_KEY.");
        }
        if (message.contains("signature") || message.contains("api_secret") || message.contains("api secret")) {
            return new CloudinaryOperationException("SIGNATURE", type,
                "Cloudinary không chấp nhận chữ ký. Kiểm tra CLOUDINARY_API_SECRET và CLOUDINARY_API_KEY thuộc cùng tài khoản.");
        }
        if (message.contains("cloud name") || message.contains("cloud_name")) {
            return new CloudinaryOperationException("CLOUD_NAME", type,
                "Cloudinary không chấp nhận cloud name. Kiểm tra CLOUDINARY_CLOUD_NAME.");
        }
        if (root instanceof javax.net.ssl.SSLException || message.contains("pkix") || message.contains("certificate")) {
            return new CloudinaryOperationException("TLS", type,
                "Không xác thực được chứng chỉ HTTPS của Cloudinary. Kiểm tra JDK/truststore và proxy của phiên STS.");
        }
        if (root instanceof java.net.UnknownHostException) {
            return new CloudinaryOperationException("DNS", type,
                "Không phân giải được máy chủ Cloudinary. Kiểm tra kết nối mạng/DNS.");
        }
        if (root instanceof java.net.SocketTimeoutException || message.contains("timed out") || message.contains("timeout")) {
            return new CloudinaryOperationException("TIMEOUT", type,
                "Kết nối Cloudinary quá thời gian chờ. Vui lòng thử lại.");
        }
        if (message.contains("invalid image") || message.contains("unsupported image") || message.contains("image file")) {
            return new CloudinaryOperationException("IMAGE_FORMAT", type,
                "Cloudinary không đọc được ảnh. Hãy chọn ảnh JPG/PNG hợp lệ và thử lại.");
        }
        if (message.contains("too large") || message.contains("file size")) {
            return new CloudinaryOperationException("IMAGE_SIZE", type,
                "Cloudinary từ chối dung lượng ảnh. Hãy chọn ảnh nhỏ hơn.");
        }
        if (root instanceof java.net.ConnectException || root instanceof java.net.SocketException) {
            return new CloudinaryOperationException("CONNECTION", type,
                "Không kết nối được Cloudinary. Kiểm tra mạng/proxy và thử lại.");
        }
        return new CloudinaryOperationException("UPLOAD_SERVICE", type,
            "Cloudinary chưa xử lý được yêu cầu. Vui lòng thử lại; xem loại lỗi trong Console.");
    }
}
