package vn.iotstar.service.impl;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.dto.ProductDTO;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.mapper.ProductMapper;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.*;
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper mapper;
    private final CloudinaryService cloudinaryService;
    @Override @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
        Math.max(page, 0), Math.min(Math.max(size, 1), 100),
        Sort.by(Sort.Direction.DESC, "id")
        );
        return productRepository.search(keyword == null ? "" : keyword, pageable)
        .map(mapper::toDTO);
    }
    @Override @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        return mapper.toDTO(productRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại")));
    }
    @Override @Transactional
    public ProductDTO create(ProductDTO dto, MultipartFile image) {
        User user = userRepository.findById(dto.getUserId())
        .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        Product product = mapper.toEntity(dto);
        product.setId(null);
        product.setImageUrl(null);
        product.setUser(user);
        if (image != null && !image.isEmpty()) {
            CloudinaryUploadResult r = cloudinaryService.upload(image);
            product.setImageUrl(r.url());
            product.setImagePublicId(r.publicId());
        }
        return mapper.toDTO(productRepository.save(product));
    }
    @Override @Transactional
    public ProductDTO update(Long id, ProductDTO dto, MultipartFile image) {
        Product product = productRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại"));
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        if (image != null && !image.isEmpty()) {
            CloudinaryUploadResult r = cloudinaryService.upload(image);
            String oldPublicId = product.getImagePublicId();
            if (oldPublicId != null) cloudinaryService.delete(oldPublicId);
            product.setImageUrl(r.url());
            product.setImagePublicId(r.publicId());
        }
        return mapper.toDTO(productRepository.save(product));
    }
    @Override @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại"));
        String publicId = product.getImagePublicId();
        if (publicId != null) cloudinaryService.delete(publicId);
        productRepository.delete(product);
    }
    @Override @Transactional(readOnly = true)
    public long countProducts() { return productRepository.count(); }
    @Override @Transactional(readOnly = true)
    public long countByUser(Long userId) {
        return productRepository.countByUserId(userId);
    }

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository, ProductMapper mapper, CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.cloudinaryService = cloudinaryService;
    }
}
