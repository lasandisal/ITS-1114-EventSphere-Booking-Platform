package lk.ijse.eventsphere.service.impl;

import lk.ijse.eventsphere.dto.CategoryRequestDTO;
import lk.ijse.eventsphere.dto.CategoryResponseDTO;
import lk.ijse.eventsphere.entity.Category;
import lk.ijse.eventsphere.exception.DuplicateResourceException;
import lk.ijse.eventsphere.exception.ResourceNotFoundException;
import lk.ijse.eventsphere.repository.CategoryRepository;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request) {
        String slug = toSlug(request.getName());
        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new DuplicateResourceException("A category with an equivalent name already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .build();
        categoryRepository.save(category);
        return toDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        String newSlug = toSlug(request.getName());
        categoryRepository.findBySlug(newSlug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("A category with an equivalent name already exists");
            }
        });

        category.setName(request.getName());
        category.setSlug(newSlug);
        categoryRepository.save(category);
        return toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        // Guard against orphaning events that still reference this category —
        // a hard FK violation would otherwise surface as an opaque 500.
        if (eventRepository.existsByCategoryId(id)) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName() + "' — events still reference it");
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponseDTO getById(Long id) {
        return categoryRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Override
    public List<CategoryResponseDTO> getAll() {
        return categoryRepository.findAll().stream().map(this::toDto).toList();
    }

    // Turns "Live Music & Concerts" into "live-music-concerts" — deterministic,
    // so re-saving the same name never produces a different slug.
    private String toSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return Pattern.compile("-{2,}").matcher(slug).replaceAll("-");
    }

    private CategoryResponseDTO toDto(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}
