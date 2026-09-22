package vn.iotstar;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.iotstar.entity.Product;
import vn.iotstar.service.CloudinaryOperationException;
import vn.iotstar.service.impl.CloudinaryServiceImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class CloudinaryUploadRegressionTest {
    @Test void invalidApiKeyLogsOnlyTypeAndSafeMessage(CapturedOutput output) throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new RuntimeException(
            "Invalid api_key FAKE-KEY-DO-NOT-LOG api_secret=FAKE-SECRET-DO-NOT-LOG"));
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(image()))
            .isInstanceOf(CloudinaryOperationException.class)
            .hasMessageContaining("CLOUDINARY_API_KEY").hasNoCause();
        assertThat(output.getAll()).contains("type=RuntimeException", "category=API_KEY", "message=")
            .doesNotContain("FAKE-KEY-DO-NOT-LOG", "FAKE-SECRET-DO-NOT-LOG");
    }

    @Test void unclassifiedResponseIsNotLoggedVerbatim(CapturedOutput output) throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("FAKE-PRIVATE-RESPONSE-DO-NOT-LOG"));
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(image()))
            .isInstanceOf(CloudinaryOperationException.class).hasNoCause();
        assertThat(output.getAll()).contains("type=IOException", "category=UPLOAD_SERVICE")
            .doesNotContain("FAKE-PRIVATE-RESPONSE-DO-NOT-LOG");
    }

    @Test void missingSecureUrlIsRejectedInsteadOfSavingNullText(CapturedOutput output) throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("public_id", "asset"));
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(image()))
            .isInstanceOf(CloudinaryOperationException.class).hasMessageContaining("secure_url/public_id");
        assertThat(output.getAll()).contains("category=INVALID_RESPONSE");
    }

    @Test void missingPublicIdIsRejected() throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.invalid/image.png"));
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(image()))
            .isInstanceOf(CloudinaryOperationException.class);
    }

    @Test void tlsAndImageErrorsHaveUsefulSafeDiagnostics() {
        assertThat(CloudinaryOperationException.from(new javax.net.ssl.SSLHandshakeException("private certificate data")).getCategory()).isEqualTo("TLS");
        assertThat(CloudinaryOperationException.from(new RuntimeException("Invalid image file private-filename")).getCategory()).isEqualTo("IMAGE_FORMAT");
        assertThat(CloudinaryOperationException.from(new RuntimeException("Invalid Signature private-signature")).getMessage())
            .contains("CLOUDINARY_API_SECRET").doesNotContain("private-signature");
    }

    @Test void oldCombinedImageRowsRemainReadableAndDeletable() {
        Product product = new Product();
        product.setImageUrl("https://example.invalid/old.png|shop/products/old");
        assertThat(product.getImageUrl()).isEqualTo("https://example.invalid/old.png");
        assertThat(product.getImagePublicId()).isEqualTo("shop/products/old");
        product.setImageUrl("https://example.invalid/new.png");
        product.setImagePublicId("shop/products/new");
        assertThat(product.getImageUrl()).isEqualTo("https://example.invalid/new.png");
        assertThat(product.getImagePublicId()).isEqualTo("shop/products/new");
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "test.png", "image/png", new byte[]{1});
    }
}
