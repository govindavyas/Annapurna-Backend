package com.localfood.backend.customer;
public record CustomerProfileResponse(String name, String phone, String address, String notes, Double latitude, Double longitude) {
    public static CustomerProfileResponse from(CustomerAccount account) {
        return new CustomerProfileResponse(account.getName(), account.getPhone(), account.getDeliveryAddress(),
            account.getDeliveryNotes(), account.getDeliveryLatitude(), account.getDeliveryLongitude());
    }
}
