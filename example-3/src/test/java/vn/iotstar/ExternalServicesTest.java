package vn.iotstar;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.service.impl.CloudinaryServiceImpl;
import vn.iotstar.service.impl.EmailServiceImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExternalServicesTest {
    @Test void emailContainsOtpSubjectRecipientAndSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "from", "shop@example.invalid");
        service.sendOtp("user@example.invalid", "123456", "Xác nhận đăng ký");
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("user@example.invalid");
        assertThat(message.getValue().getFrom()).isEqualTo("shop@example.invalid");
        assertThat(message.getValue().getSubject()).isEqualTo("Xác nhận đăng ký");
        assertThat(message.getValue().getText()).contains("123456", "5 phút");
    }

    @Test void cloudinaryRejectsEmptyAndNonImageWithoutCallingNetwork() {
        Cloudinary cloud = mock(Cloudinary.class);
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloud);
        assertThatThrownBy(() -> service.upload(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("image", new byte[0]))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("image", "file.txt", "text/plain", new byte[]{1}))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cloud);
    }

    @Test void cloudinaryRejectsOversizedImage() {
        Cloudinary cloud = mock(Cloudinary.class);
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(file)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cloud);
    }

    @Test void cloudinaryUploadMapsSecureUrlAndPublicId() throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.invalid/photo.png", "public_id", "shop/products/photo"));
        var result = new CloudinaryServiceImpl(cloud).upload(new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1}));
        assertThat(result.url()).isEqualTo("https://example.invalid/photo.png");
        assertThat(result.publicId()).isEqualTo("shop/products/photo");
        verify(uploader).upload(any(), eq(Map.of("folder", "shop/products")));
    }

    @Test void cloudinaryDeleteUsesImageResourceType() throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloud);
        service.delete(null);
        service.delete("");
        verifyNoInteractions(uploader);
        service.delete("shop/products/photo");
        verify(uploader).destroy("shop/products/photo", Map.of("resource_type", "image"));
    }

    @Test void cloudinaryFailureIsExplicit() throws IOException {
        Cloudinary cloud = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloud.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("mock failure"));
        assertThatThrownBy(() -> new CloudinaryServiceImpl(cloud).upload(new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1})))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cloudinary");
    }
}
