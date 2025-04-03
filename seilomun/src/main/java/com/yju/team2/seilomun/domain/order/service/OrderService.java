package com.yju.team2.seilomun.domain.order.service;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.domain.order.repository.OrderItemRepository;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.dto.OrderDto;
import com.yju.team2.seilomun.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateOrderNumber() {
        StringBuilder stringBuilder = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            stringBuilder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return stringBuilder.toString();
    }
    // 한가지 상품 바로 구매하기(장바구니에서 x)
    @Transactional
    public OrderDto buyProduct(OrderDto orderDto, Long customerId) {
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
        String orderNumber;
        do {
            orderNumber = generateOrderNumber();
        } while (orderRepository.existsByOrderNumber(orderNumber));

        Order order = Order.builder().
                customer(customer).
                seller(product.getSeller()).
                orderNumber(orderNumber).
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
        return orderDto;
    }
}
