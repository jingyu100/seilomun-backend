package com.yju.team2.seilomun.domain.seller.dto;

import com.yju.team2.seilomun.domain.notification.entity.NotificationPhoto;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.entity.SellerPhoto;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerInformationResponseDto {

    @NotEmpty
    private String storeName;

    private String storeDescription;

    private String notification;

    private Float rating;

    @NotEmpty
    private Character deliveryAvailable;

    private String minOrderAmount;

    private List<DeliveryFeeDto> deliveryFeeDtos;

    private String deliveryArea;

    @NotEmpty
    private String operatingHours;

    @NotEmpty
    private Long categoryId;

    @NotEmpty
    private String phone;

    @NotEmpty
    private String pickupTime;

    @NotEmpty
    private String postCode;

    @NotEmpty
    private String address;

    @NotEmpty
    private Character isOpen;

    //가게사진 추가
    private List<SellerPhotoDto> sellerPhotos;

    // 공지사진 추가
    private List<SellerPhotoDto> notificationPhotos;
    
    // 상세페이지에서 가게정보를 넘기기 위한 메서드
    public static SellerInformationResponseDto toDto(Seller seller) {
        List<DeliveryFeeDto> deliveryFeeDto = new ArrayList<>();
        if(seller.getDeliveryAvailable() == 'Y')
        {
            deliveryFeeDto = seller.getDeliveryFees().stream()
                    .map(deliveryFee -> new DeliveryFeeDto(
                            deliveryFee.getId(),
                            deliveryFee.getOrdersMoney(),
                            deliveryFee.getDeliveryTip(),
                            deliveryFee.getOrdersMoney() == 0 ? true : false
                    ))
                    .collect(Collectors.toList());
        }

        List<SellerPhotoDto> notificationPhotos = seller.getNotificationPhotos().stream()
                .map(photo -> new SellerPhotoDto(photo.getId(), photo.getPhotoUrl()))
                .collect(Collectors.toList());

        List<SellerPhotoDto> sellerPhotos = seller.getSellerPhotos().stream()
                .map(photo -> new SellerPhotoDto(photo.getId(), photo.getPhotoUrl()))
                .collect(Collectors.toList());

        if (sellerPhotos.isEmpty()) {
            sellerPhotos.add(new SellerPhotoDto(null, "/image/product1.jpg"));
        }

        return SellerInformationResponseDto.builder()
                .storeName(seller.getStoreName())
                .storeDescription(seller.getStoreDescription())
                .notification(seller.getNotification())
                .rating(seller.getRating())
                .deliveryAvailable(seller.getDeliveryAvailable())
                .minOrderAmount(seller.getMinOrderAmount())
                .deliveryFeeDtos(deliveryFeeDto)
                .deliveryArea(seller.getDeliveryArea())
                .operatingHours(seller.getOperatingHours())
                .categoryId(seller.getSellerCategory().getId())
                .phone(seller.getPhone())
                .pickupTime(seller.getPickupTime())
                .postCode(seller.getAddress())
                .address(seller.getAddressDetail())
                .isOpen(seller.getIsOpen())
                .notificationPhotos(notificationPhotos)
                .sellerPhotos(sellerPhotos)
                .build();
    }
}
