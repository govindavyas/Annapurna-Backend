package com.localfood.backend.delivery;

import com.localfood.backend.admin.AdminAuthContext;
import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.ApiException;
import com.localfood.backend.menu.MediaService;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DeliveryPaymentService {
    private final BranchService branches;
    private final AdminAuthContext auth;
    private final MediaService media;

    public DeliveryPaymentService(BranchService branches, AdminAuthContext auth, MediaService media) {
        this.branches = branches;
        this.auth = auth;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public DeliveryPaymentSettings get(UUID branchId) {
        return response(scopedBranch(branchId));
    }

    @Transactional
    public DeliveryPaymentSettings upload(UUID branchId, MultipartFile file) {
        Branch branch = branches.requireForUpdate(scopedBranch(branchId).getId());
        if (file.isEmpty() || file.getSize() > 3 * 1024 * 1024
            || !Set.of("image/png", "image/jpeg").contains(String.valueOf(file.getContentType()))) {
            throw new ApiException("Upload a PNG or JPEG QR image up to 3 MB");
        }
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ApiException("The QR file is not a valid image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!Set.of("png", "jpeg").contains(reader.getFormatName().toLowerCase())
                    || reader.getWidth(0) > 4000 || reader.getHeight(0) > 4000) {
                    throw new ApiException("Use a PNG or JPEG image no larger than 4000 pixels per side");
                }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new ApiException("The QR file is not a valid image");
        }
        branch.setDeliveryQrImageUrl(media.saveMenuImage(file));
        return response(branch);
    }

    @Transactional
    public DeliveryPaymentSettings remove(UUID branchId) {
        Branch branch = branches.requireForUpdate(scopedBranch(branchId).getId());
        branch.setDeliveryQrImageUrl(null);
        return response(branch);
    }

    private Branch scopedBranch(UUID requested) {
        UUID scope = auth.requireBranchIdForScopedAdmin();
        if (scope != null && requested != null && !scope.equals(requested)) {
            throw new ApiException("You can only access your own branch payment settings");
        }
        UUID id = scope != null ? scope : requested;
        if (id == null) throw new ApiException("Select a branch first");
        return branches.requireById(id);
    }

    private DeliveryPaymentSettings response(Branch branch) {
        return new DeliveryPaymentSettings(branch.getId(), branch.getName(), branch.getDeliveryQrImageUrl());
    }
}
