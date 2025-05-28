package com.yju.team2.seilomun.domain.notification.enums;

public enum NotificationType {

    // 소비자에게 전송할 알림
    NEW_PRODUCT("즐겨찾기한 매장에서 새 상품 등록"),
    ORDER_ACCEPTED("주문 수락"),
    ORDER_DECLINED("주문 거절"),
    ORDER_REFUND_ACCEPTED("환불 신청 수락"),
    ORDER_REFUND_DECLINED("환불 신청 거절"),
    LIKE_PRODUCT_STATUS_CHANGED("좋아요한 상품 상태가 변경"),
    CART_PRODUCT_STATUS_CHANGED("장바구니 상품 상태가 변경"),

    // 판매자에게 전송할 알림
    ORDER_OFFERED("주문 신청"),
    ORDER_REFUND("환불 신청"),
    PRODUCT_STATUS_CHANGED("등록된 상품 상태가 변경"),
    REVIEW_WRITTEN("리뷰 작성"),
    PAYMENT_COMPLETED("결제 완료");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}