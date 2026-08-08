package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.CategoryRequestDTO;
import lk.ijse.eventsphere.dto.CategoryResponseDTO;
import lk.ijse.eventsphere.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Matches SecurityConfig's /api/v1/admin/** rule (hasRole ADMIN).
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CommonResponse<CategoryResponseDTO>> create(
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), "Category created", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<CategoryResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO category = categoryService.update(id, request);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Category updated", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Category deleted", null));
    }
}
