package com.localfood.backend.menu;

import com.localfood.backend.common.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MediaService mediaService;
    private final MenuCategoryService categories;

    private final com.localfood.backend.branch.BranchRepository branches;
    private final com.localfood.backend.branch.BranchService branchService;
    private final com.localfood.backend.common.FeatureRealtimeNotifier notifier;

    public MenuService(MenuItemRepository menuItemRepository, MediaService mediaService, MenuCategoryService categories,
        com.localfood.backend.branch.BranchRepository branches, com.localfood.backend.branch.BranchService branchService,
        com.localfood.backend.common.FeatureRealtimeNotifier notifier) {
        this.categories = categories;
        this.branches = branches;
        this.branchService = branchService;
        this.notifier = notifier;
        this.menuItemRepository = menuItemRepository;
        this.mediaService = mediaService;
    }

    public List<MenuItemResponse> listAvailable() {
        var activeIds = branches.findByDeletedFalseOrderByNameAsc().stream().map(com.localfood.backend.branch.Branch::getId).collect(java.util.stream.Collectors.toSet());
        return menuItemRepository.findByAvailableTrueOrderByCategoryAscNameAsc().stream()
            .filter(item -> item.isAllBranches() || item.getBranchIds().stream().anyMatch(activeIds::contains))
            .map(MenuItemResponse::from)
            .toList();
    }

    public List<MenuItemResponse> listAll() {
        return menuItemRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .map(MenuItemResponse::from)
            .toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public MenuItemResponse create(MenuItemUpsertRequest request) {
        lockBranches();
        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        applyUpsert(item, request);
        notifier.publishAvailabilityUpdated();
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @org.springframework.transaction.annotation.Transactional
    public MenuItemResponse update(UUID menuItemId, MenuItemUpsertRequest request) {
        lockBranches();
        MenuItem item = findById(menuItemId);
        applyUpsert(item, request);
        notifier.publishAvailabilityUpdated();
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(UUID menuItemId) {
        lockBranches();
        MenuItem item = findById(menuItemId);
        menuItemRepository.delete(item);
        notifier.publishAvailabilityUpdated();
    }

    @org.springframework.transaction.annotation.Transactional
    public MenuItemResponse uploadImage(UUID menuItemId, MultipartFile file) {
        lockBranches();
        MenuItem item = findById(menuItemId);
        String imagePath = mediaService.saveMenuImage(file);
        item.setImageUrl(imagePath);
        notifier.publishAvailabilityUpdated();
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @org.springframework.transaction.annotation.Transactional
    public MenuItemResponse removeImage(UUID menuItemId) {
        lockBranches();
        MenuItem item = findById(menuItemId);
        item.setImageUrl(null);
        notifier.publishAvailabilityUpdated();
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    private void lockBranches() {
        // Order admission locks its branch first; catalogue edits use the same lock order.
        branches.findByDeletedFalseOrderByNameAsc().stream().map(com.localfood.backend.branch.Branch::getId)
            .sorted().forEach(branchService::requireForUpdate);
    }

    private MenuItem findById(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId)
            .orElseThrow(() -> new ApiException("Menu item not found"));
    }

    private void applyUpsert(MenuItem item, MenuItemUpsertRequest request) {
        String category = request.category().trim().replaceAll("\\s+", " ");
        item.setCategory(categories.ensure(category));
        if (request.allBranches() != null) item.setAllBranches(request.allBranches());
        if (request.branchIds() != null) {
            request.branchIds().forEach(branchService::requireById);
            item.getBranchIds().clear();
            item.getBranchIds().addAll(request.branchIds());
        }
        if (item.isAllBranches()) item.getBranchIds().clear();
        else if (item.getBranchIds().isEmpty()) throw new ApiException("Select at least one serving branch");
        item.setName(request.name().trim());
        item.setDescription(request.description().trim());
        item.setPricePaise(request.pricePaise());
        item.setAvailable(request.available());
        item.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
    }
}
