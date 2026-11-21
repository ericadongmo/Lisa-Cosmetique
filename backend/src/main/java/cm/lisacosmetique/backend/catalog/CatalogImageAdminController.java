package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.ProductImageResponse;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Routes ADMIN pour envoyer et gérer les images d'un produit. */
@RestController
@RequestMapping("/api/v1/admin/products/{productId}/images")
@Profile("!nodb")
@Validated
public class CatalogImageAdminController {

    private final CatalogImageAdminService imageService;

    public CatalogImageAdminController(CatalogImageAdminService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public List<ProductImageResponse> images(@PathVariable UUID productId) {
        return imageService.images(productId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> upload(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) @Size(max = 200) String altText,
            @RequestParam(defaultValue = "false") boolean primary) {
        ProductImageResponse created = imageService.upload(productId, file, altText, primary);
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/products/" + productId + "/images/" + created.id())).body(created);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID productId, @PathVariable UUID imageId) {
        imageService.delete(productId, imageId);
    }
}
