package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.CategoryResponseDTO;
import lk.ijse.eventsphere.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Public — matches SecurityConfig's permitAll() for GET /api/v1/categories/**.
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<CategoryResponseDTO>>> getAll() {
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Categories retrieved", categoryService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<CategoryResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Category retrieved", categoryService.getById(id)));
    }
}
