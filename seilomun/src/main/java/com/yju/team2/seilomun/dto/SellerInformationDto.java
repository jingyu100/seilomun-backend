package com.yju.team2.seilomun.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerInformationDto {

    @NotEmpty
    private String storeName;

    private String storeDescription;

    private String notification;

    @NotEmpty
    private Character deliveryAvailable;

    private String minOrderAmount;

    private DeliveryFeeDto deliveryFeeDto;

    private String deliveryArea;

    @NotEmpty
    private String operatingHours;

    @NotEmpty
    private String category;

    @NotEmpty
    private String phone;

    @NotEmpty
    private String pickupTime;
}
