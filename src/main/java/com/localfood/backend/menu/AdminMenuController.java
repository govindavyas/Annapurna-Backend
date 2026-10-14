package com.localfood.backend.menu;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/menu")
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public List<MenuItemResponse> listAll() {
        return menuService.listAll();
    }

    @PostMapping
    public MenuItemResponse create(@Valid @org.springframework.web.bind.annotation.RequestBody MenuItemUpsertRequest request) {
        return menuService.create(request);
    }

    @PutMapping("/{menuItemId}")
    public MenuItemResponse update(@PathVariable UUID menuItemId, @Valid @org.springframework.web.bind.annotation.RequestBody MenuItemUpsertRequest request) {
        return menuService.update(menuItemId, request);
    }

    @DeleteMapping("/{menuItemId}")
    public void delete(@PathVariable UUID menuItemId) {
        menuService.delete(menuItemId);
    }

    @PostMapping("/{menuItemId}/image")
    public MenuItemResponse uploadImage(@PathVariable UUID menuItemId, @RequestPart("file") MultipartFile file) {
        return menuService.uploadImage(menuItemId, file);
    }

    @DeleteMapping("/{menuItemId}/image")
    public MenuItemResponse removeImage(@PathVariable UUID menuItemId) {
        return menuService.removeImage(menuItemId);
    }
}
