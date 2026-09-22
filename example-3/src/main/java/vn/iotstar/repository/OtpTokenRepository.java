package vn.iotstar.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.OtpToken;
import java.util.Optional;
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpToken> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
    String email, String type);
    void deleteByEmailAndType(String email, String type);
    Optional<OtpToken> findTopByEmailAndTypeOrderByCreatedAtDesc(
    String email, String type);
}
