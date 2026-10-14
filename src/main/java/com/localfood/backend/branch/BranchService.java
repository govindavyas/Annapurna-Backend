package com.localfood.backend.branch;

import com.localfood.backend.common.ApiException;
import com.localfood.backend.common.FeatureRealtimeNotifier;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

    private final BranchRepository repository;
    private final FeatureRealtimeNotifier featureRealtimeNotifier;
    private final com.localfood.backend.admin.AdminAccountRepository accounts;
    private final com.localfood.backend.order.CustomerOrderRepository orders;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public BranchService(BranchRepository repository, FeatureRealtimeNotifier featureRealtimeNotifier,
        com.localfood.backend.admin.AdminAccountRepository accounts, com.localfood.backend.order.CustomerOrderRepository orders) {
        this.repository = repository;
        this.featureRealtimeNotifier = featureRealtimeNotifier;
        this.accounts = accounts;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listAll() {
        return repository.findByDeletedFalseOrderByNameAsc().stream().map(BranchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicBranchResponse> listPublic() {
        return repository.findByDeletedFalseOrderByNameAsc().stream().map(PublicBranchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Branch requireById(UUID branchId) {
        return repository.findById(branchId)
            .filter(branch -> !branch.isDeleted())
            .orElseThrow(() -> new ApiException("Branch not found"));
    }

    @Transactional
    public Branch requireForUpdate(UUID branchId) {
        Branch branch = repository.findForUpdate(branchId).orElseThrow(() -> new ApiException("Branch not found"));
        // Selection/authentication may already have loaded this entity before the lock.
        entityManager.refresh(branch);
        if (branch.isDeleted()) throw new ApiException("Branch not found");
        return branch;
    }

    @Transactional
    public Branch setAcceptingOrders(UUID branchId, boolean accepting) {
        Branch branch = requireForUpdate(branchId);
        branch.setShopOpen(accepting);
        featureRealtimeNotifier.publishAvailabilityUpdated();
        return branch;
    }

    @Transactional
    public void delete(UUID branchId, com.localfood.backend.admin.AdminUserDetails actor) {
        boolean allowed = accounts.lockMainAdmins().stream()
            .anyMatch(account -> account.getId().equals(actor.getId()) && account.isActive() && !account.isDeleted());
        if (!allowed) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        Branch branch = requireForUpdate(branchId);
        if (orders.existsByBranchIdAndStatusIn(branchId, List.of(
            com.localfood.backend.order.OrderStatus.PLACED, com.localfood.backend.order.OrderStatus.ACCEPTED,
            com.localfood.backend.order.OrderStatus.PREPARING, com.localfood.backend.order.OrderStatus.READY_FOR_PICKUP, com.localfood.backend.order.OrderStatus.OUT_FOR_DELIVERY))) {
            throw new ApiException("Finish or cancel this branch's unfinished orders before deleting it. You can close the shop now to stop new orders");
        }
        accounts.findByBranchIdOrderByUsernameAsc(branchId).forEach(account -> {
            account.setActive(false);
            account.setDeleted(true);
        });
        branch.setShopOpen(false);
        branch.setDeleted(true);
        featureRealtimeNotifier.publishAvailabilityUpdated();
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        repository.findByCodeIgnoreCase(request.code().trim())
            .ifPresent(existing -> {
                throw new ApiException("Branch code already exists");
            });

        Branch branch = new Branch();
        apply(branch, request);
        BranchResponse response = BranchResponse.from(repository.save(branch));
        featureRealtimeNotifier.publishAvailabilityUpdated();
        return response;
    }

    @Transactional
    public BranchResponse update(UUID branchId, BranchRequest request) {
        Branch branch = requireForUpdate(branchId);
        repository.findByCodeIgnoreCase(request.code().trim())
            .filter(existing -> !existing.getId().equals(branchId))
            .ifPresent(existing -> {
                throw new ApiException("Branch code already exists");
            });

        apply(branch, request);
        BranchResponse response = BranchResponse.from(repository.save(branch));
        featureRealtimeNotifier.publishFeaturesUpdated();
        return response;
    }

    @Transactional
    public Branch resolveBranchForOrder(UUID requestedBranchId, Double customerLat, Double customerLng) {
        if (requestedBranchId != null) {
            Branch explicit = requireForUpdate(requestedBranchId);
            if (!explicit.isShopOpen()) {
                throw new ApiException("Selected branch is currently closed");
            }
            validateDeliveryBoundary(explicit, customerLat, customerLng);
            return explicit;
        }

        List<Branch> openBranches = repository.findByShopOpenTrueAndDeletedFalseOrderByNameAsc();
        if (openBranches.isEmpty()) {
            throw new ApiException("All branches are currently closed");
        }

        if (customerLat != null && customerLng != null) {
            List<BranchDistance> candidates = openBranches.stream()
                .filter(this::hasCoordinates)
                .map(branch -> new BranchDistance(branch, haversineKm(branch.getShopLatitude(), branch.getShopLongitude(), customerLat, customerLng)))
                .filter(candidate -> candidate.distanceKm <= candidate.branch.getDeliveryRadiusKm())
                .sorted(Comparator.comparingDouble(candidate -> candidate.distanceKm))
                .toList();

            if (!candidates.isEmpty()) {
                Branch selected = requireForUpdate(candidates.get(0).branch.getId());
                validateDeliveryBoundary(selected, customerLat, customerLng);
                return selected;
            }
        }

        Branch fallback = requireForUpdate(openBranches.get(0).getId());
        validateDeliveryBoundary(fallback, customerLat, customerLng);
        return fallback;
    }

    @Transactional(readOnly = true)
    public Branch resolveBranchForFeatures(UUID requestedBranchId, Double customerLat, Double customerLng) {
        if ((customerLat == null) != (customerLng == null)
            || (customerLat != null && (!Double.isFinite(customerLat) || !Double.isFinite(customerLng)
                || Math.abs(customerLat) > 90 || Math.abs(customerLng) > 180))) {
            throw new ApiException("Provide valid latitude and longitude together");
        }
        if (requestedBranchId != null) {
            var requested = repository.findById(requestedBranchId).filter(branch -> !branch.isDeleted());
            if (requested.isPresent()) return requested.get();
        }

        List<Branch> all = repository.findByDeletedFalseOrderByNameAsc();
        List<Branch> openBranches = all.stream().filter(Branch::isShopOpen).toList();
        if (customerLat != null && customerLng != null) {
            var distances = all.stream()
                .filter(this::hasCoordinates)
                .map(branch -> new BranchDistance(branch, haversineKm(branch.getShopLatitude(), branch.getShopLongitude(), customerLat, customerLng)))
                .sorted(Comparator.comparingDouble(candidate -> candidate.distanceKm)).toList();
            return distances.stream()
                .filter(candidate -> candidate.branch.isShopOpen() && candidate.distanceKm <= candidate.branch.getDeliveryRadiusKm())
                .findFirst().or(() -> distances.stream().findFirst()).map(candidate -> candidate.branch)
                .orElseGet(() -> all.stream().findFirst().orElse(null));
        }
        return openBranches.stream().findFirst().or(() -> all.stream().findFirst()).orElse(null);
    }

    @Transactional(readOnly = true)
    public void validateDeliveryBoundary(Branch branch, Double customerLat, Double customerLng) {
        if (!branch.isShopOpen()) {
            throw new ApiException("Selected branch is currently closed");
        }

        String reason = deliveryUnavailableReason(branch, customerLat, customerLng);
        if (reason != null) throw new ApiException(reason);
    }

    public String deliveryUnavailableReason(Branch branch, Double customerLat, Double customerLng) {
        if (!hasCoordinates(branch)) return "Delivery is not available from this branch yet. Please choose another branch";
        if (customerLat == null || customerLng == null) return "Choose your delivery location on the map to check delivery availability";
        com.localfood.backend.common.Coordinates.validate(customerLat, customerLng, true);
        double distanceKm = haversineKm(branch.getShopLatitude(), branch.getShopLongitude(), customerLat, customerLng);
        if (distanceKm > branch.getDeliveryRadiusKm()) return "This address is outside this branch's delivery area. Choose a nearer branch or another address";
        return null;
    }

    private void apply(Branch branch, BranchRequest request) {
        if ((request.shopLatitude() == null) != (request.shopLongitude() == null)) {
            throw new ApiException("Branch latitude and longitude must both be provided");
        }

        if (Boolean.TRUE.equals(request.offerEnabled())) {
            if (request.offerTitle() == null || request.offerTitle().isBlank()) {
                throw new ApiException("Offer title is required when offer is enabled");
            }
            if (request.offerDescription() == null || request.offerDescription().isBlank()) {
                throw new ApiException("Offer description is required when offer is enabled");
            }
        }

        branch.setCode(request.code().trim().toUpperCase());
        branch.setName(request.name().trim());
        branch.setAddress(request.address().trim());
        branch.setDeliveryRadiusKm(request.deliveryRadiusKm());
        branch.setShopLatitude(request.shopLatitude());
        branch.setShopLongitude(request.shopLongitude());
        branch.setBannerTitle(request.bannerTitle().trim());
        branch.setBannerSubtitle(request.bannerSubtitle().trim());
        branch.setBannerImageUrl(normalizeOptional(request.bannerImageUrl()));
        branch.setOfferEnabled(Boolean.TRUE.equals(request.offerEnabled()));
        branch.setOfferTitle(normalizeOptional(request.offerTitle()));
        branch.setOfferDescription(normalizeOptional(request.offerDescription()));
        branch.setOfferCode(normalizeOptional(request.offerCode()));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasCoordinates(Branch branch) {
        return branch.getShopLatitude() != null && branch.getShopLongitude() != null;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record BranchDistance(Branch branch, double distanceKm) {
    }
}
