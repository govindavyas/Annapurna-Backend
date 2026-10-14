package com.localfood.backend.menu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
    private final MenuCategoryService service;
    public AdminCategoryController(MenuCategoryService service) { this.service=service; }
    public record CategoryRequest(@NotBlank @Size(max=100) String name) {}
    @GetMapping public List<MenuCategoryService.CategoryResponse> list() { return service.list(); }
    @PostMapping public MenuCategoryService.CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return service.create(request.name()); }
    @PutMapping public MenuCategoryService.CategoryResponse rename(@RequestParam String name, @Valid @RequestBody CategoryRequest request) { return service.rename(name,request.name()); }
    @DeleteMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String name) { service.delete(name); }
}
