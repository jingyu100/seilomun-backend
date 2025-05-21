package com.yju.team2.seilomun.domain.seller.dto;

import com.yju.team2.seilomun.domain.notification.entity.NotificationPhoto;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    
    // 공지사진 추가
    private List<String> notificationPhotos;
    // 공지사진 삭제
    private List<Long> notificationPhotoIds;
    
    // 상세페이지에서 가게정보를 넘기기 위한 메서드
    public static SellerInformationDto toDto(Seller seller) {
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


        List<String> notificationPhotos = seller.getNotificationPhotos().stream()
                .map(NotificationPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        return new SellerInformationDto(
                seller.getStoreName(),
                seller.getStoreDescription(),
                seller.getNotification(),
                seller.getDeliveryAvailable(),
                seller.getMinOrderAmount(),
                deliveryFeeDto,
                seller.getDeliveryArea(),
                seller.getOperatingHours(),
                seller.getSellerCategory().getId(),
                seller.getPhone(),
                seller.getPickupTime(),
                notificationPhotos,
                null
        );
    }
}
