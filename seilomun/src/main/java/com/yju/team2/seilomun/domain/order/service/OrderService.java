package com.yju.team2.seilomun.domain.order.service;

import com.yju.team2.seilomun.config.TossPaymentConfig;
import com.yju.team2.seilomun.domain.auth.CartItemRequestDto;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.domain.order.entity.Payment;
import com.yju.team2.seilomun.domain.order.repository.OrderItemRepository;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.order.repository.PaymentRepository;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.dto.*;
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
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ProductService productService;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PaymentRepository paymentRepository;
    private final TossPaymentConfig tossPaymentConfig;

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
        if (optionalProduct.isEmpty()){
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        Product product = optionalProduct.get();
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()){
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        Customer customer = optionalCustomer.get();

        if (product.getStockQuantity() < orderDto.getQuantity()){
            throw new IllegalArgumentException("구매 하려는 상품의 수량이 초과하였습니다.");
        }
        String orderName;
        do {
            orderName = generateOrderNumber();
        } while (orderRepository.existsByOrderName(orderName));

        Order order = Order.builder().
                customer(customer).
                seller(product.getSeller()).
                orderName(orderName).
                memo(orderDto.getMemo()).
                usedPoints(orderDto.getUsedPoints()).
                totalAmount(orderDto.getPrice() * orderDto.getQuantity()).
                isDelivery(orderDto.getIsDelivery()).
                deliveryAddress(orderDto.getDeliveryAddress()).
                deliveryStatus('N').
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

        if (orderDto.getIsDelivery().equals("Y")){
            if (orderDto.getTotalAmount() < Integer.parseInt( order.getSeller().getMinOrderAmount())){
                throw new IllegalArgumentException("배달 최소 주문금액에 부합되지 않습니다.");
            }
        }
        Payment payment = Payment.builder().
                paymentMethod(orderDto.getPayType()).
                transactionId(UUID.randomUUID().toString()).
                payName(orderDto.getOrderName()).
                totalAmount(order.getTotalAmount()).
                order(order).
                refundStatus("N").
                paySuccessYN(false).build();
        paymentRepository.save(payment);
        return payment.toPaymentResDto(customer);
    }
    // 상품 바로구매 할때 정보 페이지로 가져가기
    public OrderProductDto getBuyProduct(CartItemRequestDto cartItemRequestDto, Long customerId){
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()){
            throw new IllegalArgumentException("존재하지 않는 소비자입니다.");
        }
        Optional<Product> optionalProduct = productRepository.findById(cartItemRequestDto.getProductId());
        if (optionalProduct.isEmpty()){
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        Product product = optionalProduct.get();
        Integer currentDiscountRate = productService.getCurrentDiscountRate(product.getId());
        Integer discountPrice = product.getOriginalPrice() * (100 - currentDiscountRate) / 100;
        OrderProductDto orderProductDto = new OrderProductDto(product.getId(),cartItemRequestDto.getQuantity(),discountPrice);
        return orderProductDto;
    }
    // 요청 헤더에 토스에서 제공해준 시크릿 키를 시크릿 키를 Basic Authorization 방식으로 base64를 이용하여 인코딩하여 꼭 보내야함
    private HttpHeaders getHeaders(){
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
        try{
            result = restTemplate.postForObject(tossPaymentUrl ,
                    new HttpEntity<>(params, headers), PaymentSuccessDto.class);

        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public Payment verifyPayment(String orderId, Integer amount) {
        Payment payment = paymentRepository.findByTransactionId(orderId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));
        if (!payment.getTotalAmount().equals(amount)){
            throw new IllegalArgumentException("같은 상품 결제가 아닙니다.");
        }
        return payment;
    }
    @Transactional
    public PaymentSuccessDto tossPaymentSuccess(String paymentKey, String orderId, Integer amount) {
        Payment payment = verifyPayment(orderId, amount);
        PaymentSuccessDto paymentSuccessDto = requestPaymentAccept(paymentKey, orderId, amount);
        // 결제 성공하면 payment에 키넣고 성공으로 변환
        payment.successPayment(paymentKey);
        paymentRepository.save(payment);
        // 결제 성공하면 상품에서 물량 수량 빼기
        Optional<Order> orederOptional = orderRepository.findById(payment.getOrder().getOrId());
        if (orederOptional.isEmpty()){
            throw new IllegalArgumentException("주문 테이블이 존재 하지 않습니다");
        }
        Order order = orederOptional.get();
        Optional<OrderItem> orderItemOptional = orderItemRepository.findById(order.getOrId());
        if (orderItemOptional.isEmpty()){
            throw new IllegalArgumentException("주문 아이템이 존재 하지 않습니다");
        }
        OrderItem orderItem = orderItemOptional.get();
        Optional<Product> optionalProduct = productRepository.findById(orderItem.getId());
        if (optionalProduct.isEmpty()){
            throw new IllegalArgumentException("상품이 존재 하지 않습니다");
        }
        Product product = optionalProduct.get();
        product.updateStockQuantity(product.getStockQuantity() - orderItem.getQuantity());
        productRepository.save(product);
        return paymentSuccessDto;
    }

    //결제 실패시
    @Transactional
    public PaymentFailDto tossPaymentFail(String code,String message, String orderId){
        Payment payment = paymentRepository.findByTransactionId(orderId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));
        payment.failPayment(false);
        payment.insertFailReason(message);
        return PaymentFailDto.builder().
                errorCode(code).
                errorMessage(message).
                orderId(orderId).build();
    }
}
