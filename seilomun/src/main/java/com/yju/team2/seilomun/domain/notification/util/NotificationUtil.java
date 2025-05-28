package com.yju.team2.seilomun.domain.notification.util;

import com.yju.team2.seilomun.domain.notification.entity.Notification;
import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.entity.Payment;
import com.yju.team2.seilomun.domain.order.entity.Refund;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.review.entity.Review;

public class NotificationUtil {

    public static Notification createNotification(NotificationEvent event, Long recipientId, Character recipientType) {
        String content = generateContent(event);

        return Notification.builder()
                .content(content)
                .isVisible('Y')
                .recipientType(recipientType)
                .recipientId(recipientId)
                .senderType(event.getSenderType())
                .senderId(event.getSenderId())
                .isRead('N')
                .build();
    }

    // 모든 NotificationType에 대한 알림 내용 생성
    private static String generateContent(NotificationEvent event) {
        Object eventData = event.getEventData();

        switch (event.getType()) {

            // 소비자에게 보내는 알림들
            case NEW_PRODUCT:
                Product newProduct = (Product) eventData;
                return String.format("%s 매장에 새로운 상품 '%s'이(가) 등록되었습니다.",
                        newProduct.getSeller().getStoreName(), newProduct.getName());

            case ORDER_ACCEPTED:
                Order acceptedOrder = (Order) eventData;
                return String.format("%s 매장에서 주문을 수락했습니다. (주문번호: %s)",
                        acceptedOrder.getSeller().getStoreName(), acceptedOrder.getOrderNumber());

            case ORDER_DECLINED:
                Order declinedOrder = (Order) eventData;
                return String.format("%s 매장에서 주문을 거절했습니다. (주문번호: %s)",
                        declinedOrder.getSeller().getStoreName(), declinedOrder.getOrderNumber());

            case ORDER_REFUND_ACCEPTED:
                Refund acceptedRefund = (Refund) eventData;
                return String.format("환불 신청이 승인되었습니다. (주문번호: %s)",
                        acceptedRefund.getPayment().getOrder().getOrderNumber());

            case ORDER_REFUND_DECLINED:
                Refund declinedRefund = (Refund) eventData;
                return String.format("환불 신청이 거절되었습니다. (주문번호: %s, 사유: %s)",
                        declinedRefund.getPayment().getOrder().getOrderNumber(),
                        getRefundDeclineReason(declinedRefund));

            case LIKE_PRODUCT_STATUS_CHANGED:
                Product likedProduct = (Product) eventData;
                String likedStatusMessage = getProductStatusChangeMessage(likedProduct.getStatus());
                return String.format("좋아요한 상품 '%s'의 상태가 %s",
                        likedProduct.getName(), likedStatusMessage);

            case CART_PRODUCT_STATUS_CHANGED:
                Product cartProduct = (Product) eventData;
                String cartStatusMessage = getProductStatusChangeMessage(cartProduct.getStatus());
                return String.format("장바구니 상품 '%s'의 상태가 %s",
                        cartProduct.getName(), cartStatusMessage);

            // 판매자에게 보내는 알림들
            case ORDER_OFFERED:
                Order offeredOrder = (Order) eventData;
                return String.format("새로운 주문이 들어왔습니다. (주문번호: %s, 고객: %s, 금액: %,d원)",
                        offeredOrder.getOrderNumber(),
                        offeredOrder.getCustomer().getName(),
                        offeredOrder.getTotalAmount());

            case ORDER_REFUND:
                Refund refundRequest = (Refund) eventData;
                return String.format("환불 신청이 들어왔습니다. (주문번호: %s, 사유: %s)",
                        refundRequest.getPayment().getOrder().getOrderNumber(),
                        refundRequest.getTitle());

            case PRODUCT_STATUS_CHANGED:
                Product changedProduct = (Product) eventData;
                String sellerStatusMessage = getProductStatusChangeMessage(changedProduct.getStatus());
                return String.format("등록하신 상품 '%s'의 상태가 %s",
                        changedProduct.getName(), sellerStatusMessage);

            case REVIEW_WRITTEN:
                Review review = (Review) eventData;
                return String.format("새로운 리뷰가 작성되었습니다. (%d점, 고객: %s)",
                        review.getRating(), review.getOrder().getCustomer().getName());

            case PAYMENT_COMPLETED:
                Payment payment = (Payment) eventData;
                return String.format("결제가 완료되었습니다. (주문번호: %s, 금액: %,d원)",
                        payment.getOrder().getOrderNumber(), payment.getTotalAmount());

            default:
                return "새로운 알림이 있습니다.";
        }
    }

    // 상품 상태 변경 메시지 생성
    private static String getProductStatusChangeMessage(Character status) {
        switch (status) {
            case '0':
                return "판매 중단되었습니다.";
            case '1':
                return "판매 중으로 변경되었습니다.";
            case 'E':
                return "품절되었습니다.";
            case 'T':
                return "임박특가 기간입니다.";
            case 'X':
                return "유통기한이 만료되었습니다.";
            default:
                return "상태가 변경되었습니다.";
        }
    }

    // 주문 상태 메시지 생성 (필요한 경우 사용)
    private static String getOrderStatusMessage(Character status) {
        switch (status) {
            case 'N':
                return "주문 대기중";
            case 'A':
                return "주문 승인됨";
            case 'R':
                return "주문 거절됨";
            case 'C':
                return "주문 완료됨";
            case 'F':
                return "주문 실패됨";
            default:
                return "상태 불명";
        }
    }

    // 환불 거절 사유 추출
    private static String getRefundDeclineReason(Refund refund) {
        // 실제로는 refund.getDeclineReason() 같은 필드가 있어야 함
        // 현재는 임시로 처리
        return "자세한 사유는 매장에 문의해주세요";
    }
}