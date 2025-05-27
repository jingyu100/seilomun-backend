package com.yju.team2.seilomun.domain.order.service;

import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.dto.CartItemRequestDto;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.PointHistory;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.repository.PointHistoryRepository;
import com.yju.team2.seilomun.domain.order.dto.*;
import com.yju.team2.seilomun.domain.order.entity.*;
import com.yju.team2.seilomun.domain.order.repository.*;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.domain.review.entity.ReviewPhoto;
import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.DeliveryFeeRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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

    private String generateOrderNumber() {
        StringBuilder stringBuilder = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            stringBuilder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return stringBuilder.toString();
    }

    // 한가지 상품 바로 구매하기(장바구니에서 x)
    @Transactional
    public PaymentResDto buyProduct(OrderDto orderDto, Long customerId) {
        Optional<Product> optionalProduct = productRepository.findById(orderDto.getProductId());
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        Product product = optionalProduct.get();
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Customer customer = optionalCustomer.get();

        if (product.getStockQuantity() < orderDto.getQuantity()) {
            throw new IllegalArgumentException("구매 하려는 상품의 수량이 초과하였습니다.");
        }
        String orderNumber;
        do {
            orderNumber = generateOrderNumber();
        } while (orderRepository.existsByOrderNumber(orderNumber));

        Optional<Seller> optionalSeller = sellerRepository.findById(product.getSeller().getId());
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 판매자 입니다.");
        }
        Seller seller = optionalSeller.get();
        //배달비 목록
        List<DeliveryFee> deliveryFees = deliveryFeeRepository.findBySeller(seller);
        // 상품 구매하는 총 가격과 초기화한 배달비 값 생성
        Integer productTotalAmount = orderDto.getPrice() * orderDto.getQuantity();
        Integer deliveryFee = 0;
        // 배달 선택시 배달비 계산
        if (orderDto.getIsDelivery().equals('Y')){
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
                seller(product.getSeller()).
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
        OrderItem orderItem = OrderItem.builder().
                order(order).
                product(product).
                quantity(orderDto.getQuantity()).
                discountRate(orderDto.getCurrentDiscountRate())
                .unitPrice(orderDto.getPrice()).build();
        orderItemRepository.save(orderItem);
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
        OrderProductDto orderProductDto = new OrderProductDto(product.getId(), cartItemRequestDto.getQuantity(), discountPrice);
        return orderProductDto;
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
        Optional<Order> orederOptional = orderRepository.findById(payment.getOrder().getOrId());
        if (orederOptional.isEmpty()) {
            throw new IllegalArgumentException("주문 테이블이 존재 하지 않습니다");
        }
        Order order = orederOptional.get();
        Optional<OrderItem> orderItemOptional = orderItemRepository.findById(order.getOrId());
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
                new HttpEntity<>(params, headers),Map.class );
    }

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
        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndAndPaySuccessYN(order,true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("결제가 존재 하지 않습니다.");
        }
        Payment payment = optionalPayment.get();
        Optional<PointHistory> optionalPointHistory = pointHistoryRepository.findByOrderAndType(order,'A');
        if (optionalPointHistory.isEmpty()) {
            throw new IllegalArgumentException("포인트 적립내역 존재 하지 않습니다.");
        }
        PointHistory pointHistory = optionalPointHistory.get();
        // 환불할때 적립금이 구매했을때의 포인트보다 적으면 환불 불가
        if (customer.getPoints() >= pointHistory.getAmount()){
            payment.cancelYN(true);
            customer.minusPoint(pointHistory.getAmount());
            PointHistory pointHistory1 = PointHistory.builder().
                    type('C').  // Cancel
                            amount(pointHistory.getAmount()).
                    order(order).
                    customer(customer).
                    build();
            pointHistoryRepository.save(pointHistory1);
            // cancelReason에 테스트로 취소라 넣긴 했는데 나중에 사유 넣을것
            return tossPaymentCancel(payment.getPaymentKey(),"취소");
        }
        throw new IllegalArgumentException("결제 취소 실패");
    }
    @Transactional
    public RefundRequestDto refundApplication(Long customerId, Long orderId, RefundRequestDto refundRequestDto) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("사용자가 존재 하지 않습니다.");
        }
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("주문이 존재 하지 않습니다.");
        }
        Order order = optionalOrder.get();
        Optional<Payment> optionalPayment = paymentRepository.findByOrderAndAndPaySuccessYN(order, true);
        if (optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("결제가 존재 하지 않습니다.");
        }
        Payment payment = optionalPayment.get();
        Refund refund  = Refund.builder().
                refundType(refundRequestDto.getRefundType()).
                title(refundRequestDto.getTitle()).
                content(refundRequestDto.getContent()).
                status('N'). // 판매자가 아직 환불 수락 전이기 때문에 N
                payment(payment).
                build();
        refundRepository.save(refund);
        // 리뷰 사진 등록
        if (refundRequestDto.getRefundPhotos() != null && !refundRequestDto.getRefundPhotos().isEmpty()) {
            refundRequestDto.getRefundPhotos().forEach(url -> {
                refundPhotoRepository.save(RefundPhoto.builder()
                        .refund(refund)
                        .photoUrl(url)
                        .build());
            });
        }
        return refundRequestDto;
    }
}
