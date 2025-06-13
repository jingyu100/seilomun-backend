package com.yju.team2.seilomun.domain.customer.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.dto.*;
import com.yju.team2.seilomun.domain.customer.entity.*;
import com.yju.team2.seilomun.domain.customer.repository.*;
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
import com.yju.team2.seilomun.domain.review.entity.Review;
import com.yju.team2.seilomun.domain.review.repository.ReviewRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.util.JwtUtil;
import com.yju.team2.seilomun.util.SmsUtil;
import com.yju.team2.seilomun.validation.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final SmsUtil smsUtil;
    private final ValidationUtil validationUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ReviewRepository reviewRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private final ProductService productService;
    private final AddressRepository addressRepository;

    public Customer registerCustomer(CustomerRegisterDto customerRegisterDto) {
        String key = customerRegisterDto.getPhone();
        String storedCode = redisTemplate.opsForValue().get(key);

        if(storedCode == null || !storedCode.equals(customerRegisterDto.getVerificationCode())) {
            log.info("인증번호가 일치하지 않습니다.");
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다");
        }

        redisTemplate.delete(key);

        checkPasswordStrength(customerRegisterDto.getPassword());

        if (customerRepository.existsByEmail(customerRegisterDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        if(customerRepository.existsByPhone(customerRegisterDto.getPhone())) {
            log.info("이미 존재하는 휴대폰번호입니다.");
            throw new IllegalArgumentException("이미 등록된 휴대폰번호입니다.");
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

    public void sendValidationCode(String PhoneNumber) {
        if(customerRepository.existsByPhone(PhoneNumber)) {
            log.info("이미 존재하는 휴대폰번호입니다.");
            throw new IllegalArgumentException("이미 존재하는 휴대폰번호입니다.");
        }

        String verificationCode = validationUtil.createCode();
        smsUtil.sendOne(PhoneNumber, verificationCode);
        redisTemplate.opsForValue().set(PhoneNumber, verificationCode, Duration.ofMinutes(5));
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
    public boolean setFavorite(String email, Long sellerId) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        Customer customer = optionalCustomer.get();

        Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);
        if (optionalSeller.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 판매자입니다.");
        }
        Seller seller = optionalSeller.get();

        // 기존 즐겨찾기가 있는지 확인
        Optional<Favorite> optionalFavorite = favoriteRepository.findByCustomerAndSeller(customer, seller);

        if (optionalFavorite.isPresent()) {
            // 이미 즐겨찾기가 있으면 삭제
            Favorite existingFavorite = optionalFavorite.get();
            favoriteRepository.delete(existingFavorite);
            return false;
        } else {
            // 즐겨찾기가 없으면 추가
            Favorite favorite = Favorite.builder()
                    .customer(customer)
                    .seller(seller)
                    .build();
            favoriteRepository.save(favorite);
            return true;
        }
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
    public boolean setWishes(String email, Long productId) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        Customer customer = optionalCustomer.get();

        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        Product product = optionalProduct.get();

        // 기존 좋아요가 있는지 확인
        Optional<Wish> optionalWish = wishRepository.findByCustomerAndProduct(customer, product);

        if (optionalWish.isPresent()) {
            // 이미 좋아요가 있으면 삭제
            Wish existingWish = optionalWish.get();
            wishRepository.delete(existingWish);
            return false;
        } else {
            // 좋아요가 없으면 추가
            Wish wish = Wish.builder()
                    .customer(customer)
                    .product(product)
                    .build();
            wishRepository.save(wish);
            return true;
        }
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
                    product.getSeller().getId(),
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
            throw new IllegalArgumentException("존재하지 않는 좋아요 상품 입니다.");
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
            boolean isReview = false;
            Optional<Review> optionalReview = reviewRepository.findByOrder(order);
            if (optionalReview.isPresent()) {
                isReview = true;
            }
            OrderListResponseDto orderListResponseDto = OrderListResponseDto.builder()
                    .orderId(order.getId())
                    .sellerName(order.getSeller().getStoreName())
                    .totalAmount(order.getTotalAmount())
                    .orderDate(order.getCreatedAt())
                    .photoUrl("seller_photo_URL") // photoUrl
                    .orderStatus(order.getOrderStatus())
                    .orderItems(productNames)
                    .isReview(isReview)
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
    
    // 로컬 소비자 비밀번호검증
    public void localUserPasswordValid(Long customerId, PasswordValidDto passwordValidDto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        if(!passwordEncoder.matches(passwordValidDto.getCurrentPassword(), customer.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치 하지 않습니다.");
        }
    }
    
    // 로컬 소비자 정보 수정
    public void localUserUpdateDto(Long customerId, LocalUserUpdateDto updateDto, PasswordChangeDto passwordChangeDto) {
        if(updateDto == null) {
            throw new IllegalArgumentException("사용자 정보가 제공되지 않았습니다");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다"));

        if (!customer.getEmail().equalsIgnoreCase(updateDto.getEmail())) {
            if (customerRepository.existsByEmail(updateDto.getEmail().toLowerCase())) {
                throw new IllegalArgumentException("이미 등록된 이메일입니다.");
            }
        }

        if (!customer.getNickname().equalsIgnoreCase(updateDto.getNickname())) {
            if (customerRepository.existsByNickname(updateDto.getNickname())) {
                log.info("이미 존재하는 닉네임입니다.");
                throw new IllegalArgumentException("이미 등록된 닉네임입니다.");
            }
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

    // 소비자 주소 등록
    public void addAddress(Long customerId, AddressRequestDto address) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if(address.getAddressMain() == '1') {
            List<Address> addresses = addressRepository.findByCustomerId(customerId);
            for(Address addr : addresses) {
                if(addr.getAddressMain() == '1') {
                    addr.updateMain('0');
                    addressRepository.save(addr);
                }
            }
        }

        Address newAddress = Address.builder()
                .postCode(address.getPostCode())
                .addressDetail(address.getAddressDetail())
                .addressMain(address.getAddressMain())
                .label(address.getLabel())
                .customer(customer)
                .build();

        addressRepository.save(newAddress);
    }

    //소비자 주소 수정
    public void updateAddress(Long addressId, AddressRequestDto addressRequestDto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("주소를 찾을 수 없습니다"));

        if(addressRequestDto.getAddressMain() == '1') {
            List<Address> addresses = addressRepository.findByCustomerId(address.getCustomer().getId());
            for(Address addr : addresses) {
                if(addr.getAddressMain() == '1' && !addr.getId().equals(addressId)) {
                    addr.updateMain('0');
                    addressRepository.save(addr);
                }
            }
        }

        address.updateAddress(
                addressRequestDto.getPostCode(),
                addressRequestDto.getAddressDetail(),
                addressRequestDto.getAddressMain(),
                addressRequestDto.getLabel()
        );


        addressRepository.save(address);
    }

    // 소비자 주소 조회
    public List<Address> getAddresses(Long customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    // 소비자 주소 삭제
    public void deleteAddress(Long addressId) {
        if(!addressRepository.existsById(addressId)) {
            throw new RuntimeException("삭제할 주소가 존재하지 않습니다.");
        }

        addressRepository.deleteById(addressId);
    }


    // 포인트 적립 내역
    public PointHistoryPaginationDto getPointHistory(Long customerId, int page, int size){
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 소비자입니다.");
        }
        Customer customer = optionalCustomer.get();

        // 페이지네이션 설정 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PointHistory> pointHistorie =  pointHistoryRepository.findByCustomer(customer, pageable);
        // DTO 변환
        List<PointHistoryResDto> pointHistories = pointHistorie.getContent().stream()
                .map(PointHistoryResDto::fromEntity)
                .collect(Collectors.toList());

        return PointHistoryPaginationDto.builder()
                .pointHistories(pointHistories)
                .currentPoints(customer.getPoints())
                .hasNext(pointHistorie.hasNext())
                .totalElements(pointHistorie.getTotalElements())
                .build();
    }
}

