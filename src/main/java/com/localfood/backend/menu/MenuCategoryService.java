package com.localfood.backend.menu;
import com.localfood.backend.branch.BranchRepository;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.ApiException;
import com.localfood.backend.common.FeatureRealtimeNotifier;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class MenuCategoryService {
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;
    private final BranchRepository branches;
    private final BranchService branchService;
    private final FeatureRealtimeNotifier notifier;
    public MenuCategoryService(MenuCategoryRepository categories, MenuItemRepository items, BranchRepository branches, BranchService branchService, FeatureRealtimeNotifier notifier) {
        this.categories=categories; this.items=items; this.branches=branches; this.branchService=branchService; this.notifier=notifier;
    }
    public record CategoryResponse(String name, long productCount) {}
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(category -> new CategoryResponse(category.getName(), items.countByCategory(category.getName()))).toList();
    }
    private String clean(String value) {
        String name=value.trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length()>100) throw new ApiException("Enter a category name of 1 to 100 characters");
        return name;
    }
    private void lockCatalogue() {
        branches.findByDeletedFalseOrderByNameAsc().stream().map(com.localfood.backend.branch.Branch::getId).sorted().forEach(branchService::requireForUpdate);
    }
    @Transactional public String ensure(String value) {
        // Existing menu clients can still supply names; the catalogue remains authoritative.
        String name=clean(value);
        return categories.findByNormalizedName(name.toLowerCase(Locale.ROOT)).map(MenuCategory::getName).orElseGet(() -> insert(name).getName());
    }
    private MenuCategory insert(String name) {
        MenuCategory category=new MenuCategory(); category.setName(name); category.setNormalizedName(name.toLowerCase(Locale.ROOT));
        try { return categories.saveAndFlush(category); }
        catch (org.springframework.dao.DataIntegrityViolationException ex) { throw new ApiException("A category with this name already exists"); }
    }
    @Transactional public CategoryResponse create(String value) {
        lockCatalogue(); String name=clean(value);
        if (categories.findByNormalizedName(name.toLowerCase(Locale.ROOT)).isPresent()) throw new ApiException("A category with this name already exists");
        insert(name); return new CategoryResponse(name,0);
    }
    @Transactional public CategoryResponse rename(String original, String value) {
        lockCatalogue(); MenuCategory previous=categories.findById(original).orElseThrow(() -> new ApiException("Category not found"));
        String name=clean(value);
        if (name.equals(original)) return new CategoryResponse(name,items.countByCategory(name));
        var existing=categories.findByNormalizedName(name.toLowerCase(Locale.ROOT));
        if (existing.isPresent() && !existing.get().getName().equals(original)) throw new ApiException("A category with this name already exists");
        // Free the case-insensitive key before inserting a case-only rename.
        previous.setNormalizedName("rename-"+java.util.UUID.randomUUID()); categories.saveAndFlush(previous);
        insert(name);
        var products=items.findByCategory(original);
        products.forEach(item -> item.setCategory(name)); items.saveAllAndFlush(products);
        categories.delete(previous); categories.flush(); notifier.publishAvailabilityUpdated();
        return new CategoryResponse(name, products.size());
    }
    @Transactional public void delete(String name) {
        lockCatalogue(); var category=categories.findById(name).orElseThrow(() -> new ApiException("Category not found"));
        if (items.countByCategory(name)>0) throw new ApiException("Move this category's products to another category before deleting it");
        categories.delete(category);
    }
}
