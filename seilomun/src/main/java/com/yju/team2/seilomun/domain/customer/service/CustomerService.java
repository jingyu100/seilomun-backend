package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.dto.*;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.Favorite;
import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.customer.repository.CustomerRepository;
import com.yju.team2.seilomun.domain.customer.repository.FavoriteRepository;
import com.yju.team2.seilomun.domain.customer.repository.WishRepository;
import com.yju.team2.seilomun.domain.order.dto.OrderItemDto;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.domain.order.repository.OrderItemRepository;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.product.service.ProductService;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private final ProductService productService;

    public Customer registerCustomer(CustomerRegisterDto customerRegisterDto) {
        checkPasswordStrength(customerRegisterDto.getPassword());

        if (customerRepository.existsByEmail(customerRegisterDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        Customer customer = Customer.builder()
                .email(customerRegisterDto.getEmail())
                .password(passwordEncoder.encode(customerRegisterDto.getPassword()))
                .name(customerRegisterDto.getName())
                .phone(customerRegisterDto.getPhone())
                .nickname(customerRegisterDto.getNickname())
                .birthDate(customerRegisterDto.getBirthdate())
                .gender(customerRegisterDto.getGender())
                .profileImageUrl("default.png")
                .type('L')
                .points(0)
                .status('0')
                .deletedAt(null)
                .build();

        return customerRepository.save(customer);
    }

    private void checkPasswordStrength(String password) {
        if (PASSWORD_PATTERN.matcher(password).matches()) {
            return;
        }
        log.info("비밀번호 정책 미달");
        throw new IllegalArgumentException("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.");
    }

    // 소비자 매장 즐겨찾기 보여주기
    public FavoritePaginationDto  getFavorite(Long customerId, int page, int size) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Customer customer = optionalCustomer.get();

        // 페이지네이션을 위한 Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favoritePage = favoriteRepository.findByCustomer(customer, pageable);

        List<FavoriteSellerDto> favoriteSellerDtoList = new ArrayList<>();
        for (Favorite favorite : favoritePage.getContent()) {
            Seller seller = favorite.getSeller();
            favoriteSellerDtoList.add(new FavoriteSellerDto(
                    seller.getId(),
                    seller.getStoreName(),
                    seller.getAddressDetail(),
                    seller.getRating()
            ));
        }

        return FavoritePaginationDto.builder()
                .favorites(favoriteSellerDtoList)
                .hasNext(favoritePage.hasNext())
                .totalElements(favoritePage.getTotalElements())
                .build();
    }

    // 소비자 매장 즐겨찾기 추가
    public void setFavorite(String email, Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Customer customer = optionalCustomer.get();
        Optional<Seller> optionalSeller = sellerRepository.findById(id);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Seller seller = optionalSeller.get();
        Favorite favorite = Favorite.builder().
                customer(customer).
                seller(seller).
                build();
        favoriteRepository.save(favorite);
    }

    // 즐겨찾기 취소
    public void favoriteDelete(String email, Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Optional<Favorite> optionalFavorite = favoriteRepository.findById(id);
        if (optionalFavorite.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 즐겨찾기 입니다.");
        }
        Favorite favorite1 = optionalFavorite.get();
        favoriteRepository.delete(favorite1);
    }

    // 상품 좋아요
    public void setwishes(String email, Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Customer customer = optionalCustomer.get();
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품 입니다.");
        }
        Product product = optionalProduct.get();
        Wish wish = Wish.builder()
                .customer(customer)
                .product(product)
                .build();
        wishRepository.save(wish);
    }

    // 좋아요한 상품 조회
    public WishPaginationDto getWishedProducts(Long customerId, int page, int size) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Customer customer = optionalCustomer.get();
        // 페이지네이션을 위한 Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);
        Page<Wish> wishPage = wishRepository.findByCustomer(customer, pageable);

        List<WishProductDto> wishProductDtoList = new ArrayList<>();
        for (Wish wish : wishPage.getContent()) {
            Product product = wish.getProduct();
            Seller seller = product.getSeller();
            Integer currentDiscountRate = productService.getCurrentDiscountRate(product.getId());
            // 할인된 가격
            Integer discountPrice = product.getOriginalPrice() * (100 - currentDiscountRate) / 100;
            // 첫 번째 사진 URL 가져오기
            String photoUrl = productPhotoRepository.findTopByProductOrderById(product)
                    .map(ProductPhoto::getPhotoUrl)
                    .orElse(null);
            wishProductDtoList.add(new WishProductDto(
                    product.getId(),
                    wish.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getOriginalPrice(),
                    discountPrice,
                    currentDiscountRate,
                    product.getExpiryDate(),
                    seller.getAddressDetail(),
                    photoUrl,
                    product.getStatus()
            ));
        }

        return WishPaginationDto.builder()
                .wishes(wishProductDtoList)
                .hasNext(wishPage.hasNext())
                .totalElements(wishPage.getTotalElements())
                .build();
    }

    //상품 좋아요 취소
    public void wishDelete(String email, Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        Optional<Wish> optionalWish = wishRepository.findById(id);
        if (optionalWish.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품 입니다.");
        }
        Wish wish = optionalWish.get();
        wishRepository.delete(wish);
    }

    public Customer getUserDetailsByCustomerId(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    // 주문 목록 보기
    public OrderPaginationDto getOrderList(Long customerId, int page, int size) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Customer customer = optionalCustomer.get();
        // 페이지네이션을 위한 Pageable 객체 생성 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByCustomerIdWithPagination(customerId, pageable);

        List<OrderListResponseDto> orderListResponseDtoList = new ArrayList<>();

        for (Order order : orderPage.getContent()) {
            List<String> productNames = new ArrayList<>();
            List<OrderItem> orderItemList = orderItemRepository.findByOrder(order);

            for (OrderItem orderItem : orderItemList) {
                productNames.add(orderItem.getProduct().getName());
            }

            OrderListResponseDto orderListResponseDto = OrderListResponseDto.builder()
                    .orderId(order.getOrId())
                    .sellerName(order.getSeller().getStoreName())
                    .totalAmount(order.getTotalAmount())
                    .photoUrl("seller_photo_URL") // photoUrl
                    .orderStatus(order.getOrderStatus())
                    .orderItems(productNames)
                    .build();
            orderListResponseDtoList.add(orderListResponseDto);
        }

        return OrderPaginationDto.builder()
                .orders(orderListResponseDtoList)
                .hasNext(orderPage.hasNext())
                .totalElements(orderPage.getTotalElements())
                .build();
    }

    // 상세 주문 보기
    public OrderDetailResponseDto getOrderDetail(Long customerId, Long orderId) {
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자 입니다.");
        }
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 주문 입니다.");
        }
        Order order = optionalOrder.get();
        List<OrderItem> orderItemList = orderItemRepository.findByOrder(order);
        // 상품 정보 가져오기
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem item : orderItemList) {
            Product product = item.getProduct();
            String photoUrl = null;
            List<ProductPhoto> photos = productPhotoRepository.findByProduct(product);

            if (!photos.isEmpty()) {
                photoUrl = photos.get(0).getPhotoUrl();
            }

            OrderItemDto dto = OrderItemDto.builder()
                    .productName(product.getName())
                    .expiryDate(product.getExpiryDate())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discountRate(item.getDiscountRate())
                    .photoUrl(photoUrl)
                    .build();
            orderItemDtos.add(dto);
        }
        return OrderDetailResponseDto.builder().
                storeName(order.getSeller().getStoreName()).
                orderDate(order.getCreatedAt()).
                orderNumber(order.getOrderNumber()).
                orderItems(orderItemDtos).
                totalAmount(order.getTotalAmount()).
                usedPoint(order.getUsedPoints()).
                address(order.getDeliveryAddress()).
                deliveryFee(order.getDeliveryFee()).
                deliveryRequest(order.getMemo()).
                build();
    }

    // 소비자 정보 조회
    public LocalUserViewDto getLocalUserDto(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        return LocalUserViewDto.from(customer);
    }

    // 로컬 소비자 정보 수정
    public void localUserUpdateDto(Long customerId, LocalUserUpdateDto updateDto, PasswordChangeDto passwordChangeDto) {
        if(updateDto == null) {
            throw new IllegalArgumentException("사용자 정보가 제공되지 않았습니다");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다"));

        if (customerRepository.existsByEmail(updateDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        if (customerRepository.existsByNickname(updateDto.getNickname())) {
            log.info("이미 존재하는 닉네임입니다.");
            throw new IllegalArgumentException("이미 등록된 닉네임입니다.");
        }

        System.out.println("newPassword : " + passwordChangeDto.getNewPassword());
        System.out.println("confirmPassword : " + passwordChangeDto.getConfirmPassword());
        // 현재 비밀번호 검증
        if(!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), customer.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호와 새로운 비밀번호 검증
        if(passwordChangeDto.isValidPassword()) {
            throw new IllegalArgumentException("현재 비밀번호와 일치하므로 변경 바랍니다.");
        }

        // 새로운 비밀번호 검증
        if(!passwordChangeDto.isNewPasswordValid()) {
            throw new IllegalArgumentException("새로운 비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }

        checkPasswordStrength(passwordChangeDto.getNewPassword());
        String newPassword = passwordEncoder.encode(passwordChangeDto.getNewPassword());

        customer.UpdateLocalCustomer(updateDto,newPassword);

        customerRepository.save(customer);
    }
    
    // 소셜 소비자 정보 수정
    public void socialUserUpdateDto(Long customerId, SocialUserUpdateDto updateDto) {
        if(updateDto == null) {
            throw new IllegalArgumentException("사용자 정보가 제공되지 않았습니다");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다"));

        if(customerRepository.existsByNickname(updateDto.getNickname())) {
            log.info("이미 존재하는 닉네임입니다.");
            throw new IllegalArgumentException("이미 등록된 닉네임입니다.");
        }

        customer.UpdateSocialCustomer(updateDto);

        customerRepository.save(customer);
    }
}

