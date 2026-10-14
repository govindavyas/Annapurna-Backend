package com.localfood.backend.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "branches")
public class Branch {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "shop_open", nullable = false)
    private boolean shopOpen;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "delivery_radius_km", nullable = false)
    private Integer deliveryRadiusKm;

    @Column(name = "delivery_fee_paise", nullable = false)
    private Integer deliveryFeePaise = 3000;

    @Column(name = "minimum_order_paise", nullable = false)
    private Integer minimumOrderPaise = 0;

    @Column(name = "shop_latitude")
    private Double shopLatitude;

    @Column(name = "shop_longitude")
    private Double shopLongitude;

    @Column(name = "banner_title", nullable = false)
    private String bannerTitle;

    @Column(name = "banner_subtitle", nullable = false)
    private String bannerSubtitle;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "offer_enabled", nullable = false)
    private boolean offerEnabled;

    @Column(name = "offer_title", length = 100)
    private String offerTitle;

    @Column(name = "offer_description", length = 220)
    private String offerDescription;

    @Column(name = "offer_code", length = 40)
    private String offerCode;

    @Column(name = "delivery_qr_image_url", length = 500)
    private String deliveryQrImageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
