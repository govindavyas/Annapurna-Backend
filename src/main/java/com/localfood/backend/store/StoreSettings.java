package com.localfood.backend.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_settings")
public class StoreSettings {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "shop_open", nullable = false)
    private boolean shopOpen;

    @Column(name = "delivery_radius_km", nullable = false)
    private Integer deliveryRadiusKm;

    @Column(name = "shop_latitude")
    private Double shopLatitude;

    @Column(name = "shop_longitude")
    private Double shopLongitude;

    @Column(name = "banner_title", nullable = false)
    private String bannerTitle;

    @Column(name = "banner_subtitle", nullable = false)
    private String bannerSubtitle;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "offer_enabled", nullable = false)
    private boolean offerEnabled;

    @Column(name = "offer_title")
    private String offerTitle;

    @Column(name = "offer_description")
    private String offerDescription;

    @Column(name = "offer_code")
    private String offerCode;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = SINGLETON_ID;
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
