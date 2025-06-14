package com.yju.team2.seilomun.domain.order.service;

import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.dto.CartItemRequestDto;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.PointHistory;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.repository.PointHistoryRepository;
import com.yju.team2.seilomun.domain.notification.event.NewProductEvent;
import com.yju.team2.seilomun.domain.notification.event.OrderAcceptedEvent;
import com.yju.team2.seilomun.domain.notification.event.OrderDeclinedEvent;
import com.yju.team2.seilomun.domain.notification.event.OrderRefundAcceptedEvent;
import com.yju.team2.seilomun.domain.notification.service.NotificationService;
import com.yju.team2.seilomun.domain.order.dto.*;
import com.yju.team2.seilomun.domain.order.entity.*;
import com.yju.team2.seilomun.domain.order.repository.*;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.domain.review.entity.Review;
import com.yju.team2.seilomun.domain.review.repository.ReviewRepository;
import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.DeliveryFeeRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.upload.service.AWSS3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ProductService productService;
    private final PointHistoryRepository pointHistoryRepository;
    private final SellerRepository sellerRepository;
    private final DeliveryFeeRepository deliveryFeeRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PaymentRepository paymentRepository;
    private final TossPaymentConfig tossPaymentConfig;
    private final RefundPhotoRepository refundPhotoRepository;
    private final RefundRepository refundRepository;
    private final NotificationService notificationService;
    private final AWSS3UploadService awss3UploadService;
    private final ReviewRepository reviewRepository;

    private String generateOrderNumber() {
        StringBuilder stringBuilder = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            stringBuilder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return stringBuilder.toString();
    }

    // 상품  구매하기
    @Transactional
    public PaymentResDto buyProduct(OrderDto orderDto, Long customerId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Customer customer = optionalCustomer.get();

        if (orderDto.getOrderProducts() == null || orderDto.getOrderProducts().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품 정보가 없습니다.");
        }
        List<Product> products = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        Integer productTotalAmount = 0;
        Seller seller = null;
        for (OrderProductDto productDto : orderDto.getOrderProducts()) {
            Optional<Product> optionalProduct = productRepository.findById(productDto.getProductId());
            if (optionalProduct.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 상품입니다: " + productDto.getProductId());
            }
            Product product = optionalProduct.get();
            products.add(product);

            // 모든 상품이 같은 판매자인지 확인
            if (seller == null) {
                seller = product.getSeller();
            } else if (!seller.getId().equals(product.getSeller().getId())) {
                throw new IllegalArgumentException("다른 판매자의 상품들은 함께 주문할 수 없습니다.");
            }

            // 재고 확인
            if (product.getStockQuantity() < productDto.getQuantity()) {
                throw new IllegalArgumentException("구매 하려는 상품의 수량이 초과하였습니다: " + product.getName());
            }

            // 총 상품 금액 누적
            productTotalAmount += productDto.getPrice() * productDto.getQuantity();

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(productDto.getQuantity())
                    .discountRate(productDto.getCurrentDiscountRate())
                    .unitPrice(productDto.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        String orderNumber;
        do {
            orderNumber = generateOrderNumber();
        } while (orderRepository.existsByOrderNumber(orderNumber));

        //배달비 목록
        List<DeliveryFee> deliveryFees = deliveryFeeRepository.findBySeller(seller);
        Integer deliveryFee = 0;
        // 배달 선택시 배달비 계산
        if (orderDto.getIsDelivery().equals('Y')) {
            // 판매자 배달 가능 여부 확인
            if (seller.getDeliveryAvailable() != 'Y') {
                throw new IllegalArgumentException("해당 판매자는 배달 서비스를 제공하지 않습니다.");
            }
            // 최소 주문 금액 확인
            if (seller.getMinOrderAmount() != null && productTotalAmount < Integer.parseInt(seller.getMinOrderAmount())) {
                throw new IllegalArgumentException("배달 최소 주문금액(" + seller.getMinOrderAmount() + "원)에 부합되지 않습니다.");
            }

            // 배달비 찾기
            DeliveryFee applicableFee = null;
            // 오름차순으로 정렬
            List<DeliveryFee> sortedFees = new ArrayList<>(deliveryFees);
            sortedFees.sort(Comparator.comparing(DeliveryFee::getOrdersMoney));

            // 주문 금액보다 작거나 같은 가장 큰 ordersMoney 찾기
            for (int i = sortedFees.size() - 1; i >= 0; i--) {
                DeliveryFee fee = sortedFees.get(i);
                if (fee.getOrdersMoney() <= productTotalAmount) {
                    applicableFee = fee;
                    break;
                }
            }

            if (applicableFee != null) {
                deliveryFee = applicableFee.getDeliveryTip();
            } else {
                // 적용 가능한 구간이 없으면 가장 낮은 구간 적용
                if (!sortedFees.isEmpty()) {
                    deliveryFee = sortedFees.get(0).getDeliveryTip();
                }
            }

        }

        // 최종 주문 금액에 배달비 추가
        Integer totalAmount = productTotalAmount + deliveryFee;

        Order order = Order.builder().
                customer(customer).
                seller(seller).
                orderNumber(orderNumber).
                memo(orderDto.getMemo()).
                usedPoints(orderDto.getUsedPoints()).
                totalAmount(totalAmount).
                isDelivery(orderDto.getIsDelivery()).
                deliveryAddress(orderDto.getDeliveryAddress()).
                deliveryStatus('N').
                deliveryFee(deliveryFee).
                isReivewed('N').
                orderStatus('N').build();
        orderRepository.save(order);
        for (OrderItem orderItem : orderItems) {
            orderItem = OrderItem.builder()
                    .order(order)
                    .product(orderItem.getProduct())
                    .quantity(orderItem.getQuantity())
                    .discountRate(orderItem.getDiscountRate())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
            orderItemRepository.save(orderItem);
        }
        // 주문 가격이랑 포인트를 합산한채로 결제
        Integer totalAmountMinusPoint = order.getTotalAmount() - order.getUsedPoints();

        Payment payment = Payment.builder().
                paymentMethod(orderDto.getPayType()).
                transactionId(UUID.randomUUID().toString()).
                payName(orderDto.getOrderName()).
                totalAmount(totalAmountMinusPoint).
                order(order).
                refundStatus("N").
                paySuccessYN(false).build();
        paymentRepository.save(payment);
        return payment.toPaymentResDto(customer);
    }

    // 상품 바로구매 할때 정보 페이지로 가져가기
    public OrderProductDto getBuyProduct(CartItemRequestDto cartItemRequestDto, Long customerId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자입니다.");
        }
        Optional<Product> optionalProduct = productRepository.findById(cartItemRequestDto.getProductId());
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        Product product = optionalProduct.get();
        Integer currentDiscountRate = productService.getCurrentDiscountRate(product.getId());
        Integer discountPrice = product.getOriginalPrice() * (100 - currentDiscountRate) / 100;
        OrderProductDto orderProductDto = new OrderProductDto(product.getId(), cartItemRequestDto.getQuantity(), discountPrice, currentDiscountRate);
        return orderProductDto;
    }

    // 장바구니에서 상품 구매갈 때
    public List<OrderProductDto> getBuyProducts(List<CartItemRequestDto> cartItems, Long customerId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자입니다.");
        }

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품 정보가 없습니다.");
        }

        List<OrderProductDto> orderProducts = new ArrayList<>();

        for (CartItemRequestDto cartItem : cartItems) {
            Optional<Product> optionalProduct = productRepository.findById(cartItem.getProductId());
            if (optionalProduct.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 상품입니다: " + cartItem.getProductId());
            }
            Product product = optionalProduct.get();
            Integer currentDiscountRate = productService.getCurrentDiscountRate(product.getId());
            Integer discountPrice = product.getOriginalPrice() * (100 - currentDiscountRate) / 100;

            OrderProductDto orderProductDto = new OrderProductDto(
                    product.getId(),
                    cartItem.getQuantity(),
                    discountPrice,
                    currentDiscountRate
            );
            orderProducts.add(orderProductDto);
        }

        return orderProducts;
    }

    // 요청 헤더에 토스에서 제공해준 시크릿 키를 시크릿 키를 Basic Authorization 방식으로 base64를 이용하여 인코딩하여 꼭 보내야함
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String encodedAuthKey = new String(
                Base64.getEncoder().encode((tossPaymentConfig.getTestSecreteKey() + ":").getBytes(StandardCharsets.UTF_8)));
        headers.setBasicAuth(encodedAuthKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    // 토스페이먼츠에 최종 결제 승인 요청을 보내기 위해 필요한 정보들을 담아 POST로 보내는 부분
    @Transactional
    public PaymentSuccessDto requestPaymentAccept(String paymentKey, String orderId, Integer amount) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHeaders();
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("amount", amount);

        String tossPaymentUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey;
        PaymentSuccessDto result = null;
        try {
            result = restTemplate.postForObject(tossPaymentUrl,
                    new HttpEntity<>(params, headers), PaymentSuccessDto.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Payment verifyPayment(String orderId, Integer amount) {
        Payment payment = paymentRepository.findByTransactionId(orderId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));
        if (!payment.getTotalAmount().equals(amount)) {
            throw new IllegalArgumentException("같은 상품 결제가 아닙니다.");
        }
        return payment;
    }

    @Transactional
    public PaymentSuccessDto tossPaymentSuccess(String paymentKey, String orderId, Integer amount) {
        Payment payment = verifyPayment(orderId, amount);
        // 결제 성공하면 상품에서 물량 수량 빼기
        Optional<Order> orederOptional = orderRepository.findById(payment.getOrder().getId());
        if (orederOptional.isEmpty()) {
            throw new IllegalArgumentException("주문 테이블이 존재 하지 않습니다");
        }
        Order order = orederOptional.get();
        Optional<OrderItem> orderItemOptional = orderItemRepository.findById(order.getId());
        if (orderItemOptional.isEmpty()) {
            throw new IllegalArgumentException("주문 아이템이 존재 하지 않습니다");
        }
        OrderItem orderItem = orderItemOptional.get();
        Optional<Product> optionalProduct = productRepository.findById(orderItem.getProduct().getId());
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("상품이 존재 하지 않습니다");
        }
        PaymentSuccessDto paymentSuccessDto = requestPaymentAccept(paymentKey, orderId, amount);
        // 결제 성공하면 payment에 키넣고 성공으로 변환
        payment.successPayment(paymentKey);
        paymentRepository.save(payment);
        Product product = optionalProduct.get();
        product.updateStockQuantity(product.getStockQuantity() - orderItem.getQuantity());
        productRepository.save(product);

        //결제 성공하면 유저 point 1퍼센트 증가
        Optional<Customer> optionalCustomer = customerRepository.findById(order.getCustomer().getId());
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("사용자가 존재 하지 않습니다.");
        }
        Customer customer = optionalCustomer.get();
        Integer getPoint = payment.getTotalAmount() / 100;
        customer.buyProductAddPoint(getPoint);
        customerRepository.save(customer);

        PointHistory pointHistory = PointHistory.builder().
                type('A').  // ADD
                        amount(getPoint).
                order(order).
                customer(customer).
                build();
        pointHistoryRepository.save(pointHistory);
        order.updateOrderStatus('S');
        orderRepository.save(order);
        return paymentSuccessDto;
    }

    //결제 실패시
    @Transactional
    public PaymentFailDto tossPaymentFail(String code, String message, String orderId) {
        Payment payment = paymentRepository.findByTransactionId(orderId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));
        payment.failPayment(false);
        payment.insertFailReason(message);
        Optional<Order> optionalOrder = orderRepository.findByOrderNumber(orderId);
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        order.updateOrderStatus('F');   // fail의 F
        restoreStock(order);
        orderRepository.save(order);
        return PaymentFailDto.builder().
                errorCode(code).
                errorMessage(message).
                orderId(orderId).build();
    }

    // 토스 결제 취소 승인 요청
    public Map tossPaymentCancel(String paymentKey, String cancelReason) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHeaders();
        JSONObject params = new JSONObject();
        params.put("cancelReason", cancelReason);

        return restTemplate.postForObject(TossPaymentConfig.URL + paymentKey + "/cancel",
                new HttpEntity<>(params, headers), Map.class);
    }

    // 소비자 결제 취소
    @Transactional
    public Map cancelPayment(Long customerId, Long orderId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("사용자가 존재 하지 않습니다.");
        }
        Customer customer = optionalCustomer.get();
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndPaySuccessYN(order, true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("결제가 존재 하지 않습니다.");
        }
        Payment payment = optionalPayment.get();
        Optional<PointHistory> optionalPointHistory = pointHistoryRepository.findByOrderAndType(order, 'A');
        if (optionalPointHistory.isEmpty()) {
            throw new IllegalArgumentException("포인트 적립내역 존재 하지 않습니다.");
        }
        PointHistory pointHistory = optionalPointHistory.get();
        // 환불할때 적립금이 구매했을때의 포인트보다 적으면 환불 불가
        if (customer.getPoints() >= pointHistory.getAmount()) {
            payment.cancelYN(true);
            customer.minusPoint(pointHistory.getAmount());
            PointHistory pointHistory1 = PointHistory.builder().
                    type('C').  // Cancel
                            amount(pointHistory.getAmount()).
                    order(order).
                    customer(customer).
                    build();
            pointHistoryRepository.save(pointHistory1);
            order.updateOrderStatus('C');
            restoreStock(order);
            orderRepository.save(order);
            // cancelReason에 테스트로 취소라 넣긴 했는데 나중에 사유 넣을것
            return tossPaymentCancel(payment.getPaymentKey(), "취소");
        }
        throw new IllegalArgumentException("결제 취소 실패");
    }

    // 결체장 닫으면 부를 메서드
    @Transactional
    public void closePayment(Long customerId, Long orderId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("사용자가 존재하지 않습니다.");
        }
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId, 'N');
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("대기 중인 주문이 존재하지 않습니다.");
        }

        Order order = optionalOrder.get();
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }
        Optional<Payment> optionalPayment = paymentRepository.findByOrder(order);
        if (optionalPayment.isEmpty()) {
            log.warn("결제 정보가 없는 주문 삭제: orderId={}", order.getId());
            orderRepository.delete(order);
        }
        Payment payment = optionalPayment.get();
        if (payment.isPaySuccessYN()) {
            throw new IllegalArgumentException("이미 결제가 완료된 주문입니다. 환불 신청을 해주세요.");
        }
        try {
            paymentRepository.delete(payment);
            orderRepository.delete(order);

        } catch (Exception e) {
            throw new RuntimeException("주문 삭제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //환불 신청
    @Transactional
    public RefundRequestDto refundApplication(Long customerId, Long orderId, RefundRequestDto refundRequestDto,List<MultipartFile> photos) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("사용자가 존재 하지 않습니다.");
        }
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId, 'A');
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("판매자가 수락한 주문이 아닙니다");
        }
        Order order = optionalOrder.get();
        // 환불 신청자가 주문자와 동일한지 확인
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }

        //리뷰가 작성된 경우 환불 신청 불가
        Optional<Review> optionalReview = reviewRepository.findByOrder(order);
        if (optionalReview.isPresent()) {
            throw new IllegalArgumentException("리뷰가 작성된 주문은 환불 신청이 불가능합니다.");
        }

        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndPaySuccessYN(order, true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("결제가 존재 하지 않습니다.");
        }
        Payment payment = optionalPayment.get();
        // 사진 업로드 처리
        List<String> photoUrls = new ArrayList<>();
        if (photos != null && !photos.isEmpty()) {
            if (photos.size() > 5) {
                throw new IllegalArgumentException("사진은 최대 5장까지 업로드할 수 있습니다.");
            }

            try {
                photoUrls = awss3UploadService.uploadFiles(photos);
                log.info("환불 신청 사진 업로드 완료: {} 장", photoUrls.size());
            } catch (Exception e) {
                log.error("환불 신청 사진 업로드 실패: {}", e.getMessage());
                throw new RuntimeException("사진 업로드에 실패했습니다.");
            }
        }

        Refund refund = Refund.builder().
                refundType(refundRequestDto.getRefundType()).
                title(refundRequestDto.getTitle()).
                content(refundRequestDto.getContent()).
                status('N'). // 판매자가 아직 환불 수락 전이기 때문에 N
                payment(payment).
                build();
        refundRepository.save(refund);

        // 환불 사진 등록
        List<String> allPhotoUrls = new ArrayList<>();
        if (refundRequestDto.getRefundPhotos() != null) {
            allPhotoUrls.addAll(refundRequestDto.getRefundPhotos());
        }
        allPhotoUrls.addAll(photoUrls);

        if (!allPhotoUrls.isEmpty()) {
            allPhotoUrls.forEach(url -> {
                refundPhotoRepository.save(RefundPhoto.builder()
                        .refund(refund)
                        .photoUrl(url)
                        .build());
            });
        }
        refundRequestDto.setRefundPhotos(allPhotoUrls);
        return refundRequestDto;
    }

    //판매자 주문 수락 메서드
    @Transactional
    public void acceptanceOrder(Long sellerId, Long orderId) {
        //수락 전 인 것만 찾기
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId, 'S');
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("없는 판매자 입니다.");
        }
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }
        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndPaySuccessYN(order, true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("성공한 결제가 아닙니다.");
        }
        order.updateOrderStatus('A'); //acceptance의 a
        orderRepository.save(order);

        // 결제한 고객에게 알림 전송
        try {
            if (notificationService != null) {
                OrderAcceptedEvent orderAcceptedEvent = OrderAcceptedEvent.builder()
                        .order(order)
                        .eventId("ORDER_ACCEPTED_" + order.getId())
                        .build();

                notificationService.processNotification(orderAcceptedEvent);
                log.info("주문 수락 알림 전송 완료");
            }
        } catch (Exception e) {
            log.error("주문 수락 알림 전송 실패", e);
            // 알림 전송 실패해도
        }

    }

    //판매자 주문 거절 메서드
    @Transactional
    public void refuseOrder(Long sellerId, Long orderId) {
        //수락 전 인 것만 찾기
        Optional<Order> optionalOrder = orderRepository.findByIdAndOrderStatus(orderId, 'S');
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("없는 판매자 입니다.");
        }
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }
        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndPaySuccessYN(order, true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("성공한 결제가 아닙니다.");
        }
        //주문 거절 했으니 결제 취소(환불)
        cancelPayment(order.getCustomer().getId(), order.getId());
        order.updateOrderStatus('R'); //refuse의 r
        restoreStock(order);
        orderRepository.save(order);

        // 알림 전송
        try {
            if (notificationService != null) {
                OrderDeclinedEvent orderDeclinedEvent = OrderDeclinedEvent.builder()
                        .order(order)
                        .eventId("ORDER_DECLINED_" + order.getId())
                        .build();

                notificationService.processNotification(orderDeclinedEvent);
                log.info("주문 거절 알림 전송 완료");
            }
        } catch (Exception e) {
            log.error("주문 거절 알림 전송 실패", e);
            // 알림 전송 실패해도
        }
    }

    //환불 수락
    @Transactional
    public void refundAcceptance(Long sellerId, Long refundId) {
        Optional<Refund> optionalRefund = refundRepository.findByIdAndStatus(refundId, 'N');
        if (optionalRefund.isEmpty()) {
            throw new IllegalArgumentException("환불 신청이 존재 하지 않습니다.");
        }
        Refund refund = optionalRefund.get();
        Optional<Payment> optionalPayment = paymentRepository.findByIdAndPaySuccessYN(refund.getPayment().getId(), true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("결제 내역이 존재 하지 않습니다.");
        }
        Payment payment = optionalPayment.get();
        Optional<Order> optionalOrder = orderRepository.findById(payment.getOrder().getId());
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문 내역이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }
        //결제 취소
        cancelPayment(order.getCustomer().getId(), order.getId());
        refund.insertProcessedAt(LocalDateTime.now());
        refund.updateStatus('A'); // 수락의 A
        order.updateOrderStatus('B');
        restoreStock(order);
        refundRepository.save(refund);
        orderRepository.save(order);

        // 알림 전송
        try {
            if (notificationService != null) {
                OrderRefundAcceptedEvent orderRefundAcceptedEvent = OrderRefundAcceptedEvent.builder()
                        .refund(refund)
                        .eventId("REFUND_ACCEPTED_" + refund.getId())
                        .build();

                notificationService.processNotification(orderRefundAcceptedEvent);
                log.info("환불 수락 알림 전송 완료");
            }
        } catch (Exception e) {
            log.error("환불 수락 알림 전송 실패", e);
            // 알림 전송 실패
        }
    }

    //통계
    public List<StatsDto> getStats(Long id, int year, int month) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        return orderRepository.stats(seller.getId(), year, month);
    }


    // 재고 복구 로직을 별도 메서드로
    private void restoreStock(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            int newStock = product.getStockQuantity() + orderItem.getQuantity();
            product.updateStockQuantity(newStock);
            productRepository.save(product);

            log.info("재고 복구: productId={}, 복구량={}, 새로운재고={}",
                    product.getId(), orderItem.getQuantity(), newStock);
        }
    }
}
