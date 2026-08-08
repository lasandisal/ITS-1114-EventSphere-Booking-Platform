package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.CategoryRequestDTO;
import lk.ijse.eventsphere.dto.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {
    CategoryResponseDTO create(CategoryRequestDTO request);
    CategoryResponseDTO update(Long id, CategoryRequestDTO request);
    void delete(Long id);
    CategoryResponseDTO getById(Long id);
    List<CategoryResponseDTO> getAll();
}
