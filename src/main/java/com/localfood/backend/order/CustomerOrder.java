package com.localfood.backend.order;

import com.localfood.backend.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "orders")
public class CustomerOrder {

    @Id
    private UUID id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @Column(name = "delivery_notes", length = 300)
    private String deliveryNotes;
    private UUID dispatchedBy;
    private OffsetDateTime dispatchedAt;
    private Double dispatchLatitude;
    private Double dispatchLongitude;
    private String dispatchLocationSource;
    private OffsetDateTime estimatedArrivalFrom;
    private OffsetDateTime estimatedArrivalTo;
    private Double estimatedDistanceKm;
    private Double riderLatitude;
    private Double riderLongitude;
    private OffsetDateTime riderLocationAt;
    private OffsetDateTime riderLocationReceivedAt;
    private Double riderAccuracyMeters;
    @Column(nullable = false)
    private boolean riderSharing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "subtotal_paise", nullable = false)
    private Integer subtotalPaise;

    @Column(name = "delivery_fee_paise", nullable = false)
    private Integer deliveryFeePaise;

    @Column(name = "total_paise", nullable = false)
    private Integer totalPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", length = 20)
    private CollectionMethod collectionMethod;

    @Column(name = "collected_at")
    private OffsetDateTime collectedAt;

    @Column(name = "collected_by")
    private UUID collectedBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
