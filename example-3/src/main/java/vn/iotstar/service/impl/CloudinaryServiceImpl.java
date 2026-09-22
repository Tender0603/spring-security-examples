package vn.iotstar.service.impl;
import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.service.CloudinaryService;
import vn.iotstar.service.CloudinaryUploadResult;
import vn.iotstar.service.CloudinaryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
@Service
public class CloudinaryServiceImpl implements CloudinaryService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);
    private final Cloudinary cloudinary;
    @Override
    public CloudinaryUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty())
        throw new IllegalArgumentException("Chưa chọn ảnh");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Ảnh vượt quá 10 MB");
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/"))
        throw new IllegalArgumentException("Chỉ cho phép file hình ảnh");
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            Map.of("folder", "shop/products")
            );
            if (result == null || !(result.get("secure_url") instanceof String url)
                    || !url.startsWith("https://") || !(result.get("public_id") instanceof String publicId)
                    || publicId.isBlank()) {
                throw CloudinaryOperationException.invalidResponse();
            }
            return new CloudinaryUploadResult(url, publicId);
        } catch (Exception e) {
            throw report("upload", e);
        }
    }
    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(
            publicId, Map.of("resource_type", "image")
            );
        } catch (Exception e) {
            throw report("delete", e);
        }
    }

    private CloudinaryOperationException report(String operation, Exception error) {
        CloudinaryOperationException safe = error instanceof CloudinaryOperationException known
            ? known : CloudinaryOperationException.from(error);
        // No exception argument, stack trace, response body, file name or config values.
        log.error("Cloudinary {} failed: type={}, category={}, message={}",
            operation, safe.getFailureType(), safe.getCategory(), safe.getMessage());
        return safe;
    }

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }
}
