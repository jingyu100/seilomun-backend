package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.notification.entity.NotificationPhoto;
import com.yju.team2.seilomun.domain.notification.repository.NotificationPhotoRepositry;
import com.yju.team2.seilomun.domain.order.dto.OrderItemDto;
import com.yju.team2.seilomun.domain.order.entity.*;
import com.yju.team2.seilomun.domain.order.repository.OrderItemRepository;
import com.yju.team2.seilomun.domain.order.repository.OrderRepository;
import com.yju.team2.seilomun.domain.order.repository.PaymentRepository;
import com.yju.team2.seilomun.domain.order.repository.RefundRepository;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.seller.dto.*;
import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.entity.SellerCategoryEntity;
import com.yju.team2.seilomun.domain.seller.entity.SellerPhoto;
import com.yju.team2.seilomun.domain.seller.repository.DeliveryFeeRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerCategoryRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerPhotoRepository;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.upload.service.AWSS3UploadService;
import com.yju.team2.seilomun.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerService {
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private final SellerRepository sellerRepository;
    private final SellerPhotoRepository sellerPhotoRepository;
    private final DeliveryFeeRepository deliveryFeeRepository;
    private final SellerCategoryRepository sellerCategoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final SellerIndexService sellerIndexService;
    private final AWSS3UploadService awsS3UploadService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final NotificationPhotoRepositry notificationPhotoRepositry;

    // 판매자 가입
    public Seller sellerRegister(SellerRegisterDto sellerRegisterDto) {
        checkPasswordStrength(sellerRegisterDto.getPassword());

        if (sellerRepository.existsByEmail(sellerRegisterDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다.");
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        SellerCategoryEntity category = sellerCategoryRepository.findById(sellerRegisterDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리입니다"));

        Seller seller = Seller.builder()
                .email(sellerRegisterDto.getEmail())
                .password(passwordEncoder.encode(sellerRegisterDto.getPassword()))
                .businessNumber(sellerRegisterDto.getBusinessNumber())
                .storeName(sellerRegisterDto.getStoreName())
                .addressDetail(sellerRegisterDto.getAddressDetail())
                .phone(sellerRegisterDto.getPhone())
                //여기서부턴 임시
                .sellerCategory(category)
                .storeDescription("가게 설명")
                .status('1')
                .address(sellerRegisterDto.getAddress())
                .operatingHours("12")
                .deliveryAvailable('0')
                .rating(0F)
                .pickupTime("30분")
                .isOpen('0')
                .build();

        Seller savedSeller = sellerRepository.save(seller);

        // Elasticsearch에 가게 정보 인덱싱
        sellerIndexService.indexSeller(savedSeller);

        return savedSeller;
    }

    // 유저 정보 업데이트 (사진 추가는 아직 x)
    public Seller updateSellerInformation(String email, SellerInformationDto sellerInformationDto, List<MultipartFile> storeImage, List<MultipartFile> notificationImage) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        SellerCategoryEntity sellerCategory = sellerCategoryRepository.findById(sellerInformationDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        seller.updateInformation(sellerInformationDto, sellerCategory);

        List<DeliveryFeeDto> deliveryFeeDtos = sellerInformationDto.getDeliveryFeeDtos();
        if (deliveryFeeDtos != null && !deliveryFeeDtos.isEmpty()) {
            for (DeliveryFeeDto deliveryFeeDto : deliveryFeeDtos) {
                // ID가 있는 경우 기존 배달비 정보 업데이트 또는 삭제
                if (deliveryFeeDto.getId() != null) {
                    Optional<DeliveryFee> optionalDeliveryFee = deliveryFeeRepository.findById(deliveryFeeDto.getId());
                    if (optionalDeliveryFee.isEmpty()) {
                        throw new IllegalArgumentException("존재하지 않는 배달비 정보입니다: ID=" + deliveryFeeDto.getId());
                    }

                    DeliveryFee deliveryFee = optionalDeliveryFee.get();
                    // 자기게 맞는지
                    if (!deliveryFee.getSeller().getId().equals(seller.getId())) {
                        throw new IllegalArgumentException("권한이 없습니다: ID=" + deliveryFeeDto.getId());
                    }
                    // 삭제 버튼 눌렀으면
                    if (Boolean.TRUE.equals(deliveryFeeDto.getDeleted())) {
                        deliveryFeeRepository.delete(deliveryFee);
                        log.info("배달비 정보가 삭제되었습니다: ID={}", deliveryFee.getId());
                    } else {
                        deliveryFee.updateInformation(deliveryFeeDto);
                        deliveryFeeRepository.save(deliveryFee);
                        log.info("배달비 정보가 업데이트되었습니다: ID={}", deliveryFee.getId());
                    }
                }
                // id가 없고 입력값이 있으면 새로만듬
                else if (deliveryFeeDto.getDeliveryTip() != null && deliveryFeeDto.getOrdersMoney() != null) {
                    DeliveryFee deliveryFee = DeliveryFee.builder()
                            .ordersMoney(deliveryFeeDto.getOrdersMoney())
                            .deliveryTip(deliveryFeeDto.getDeliveryTip())
                            .seller(seller)
                            .build();
                    deliveryFeeRepository.save(deliveryFee);
                    log.info("새 배달비 정보가 추가되었습니다: 판매자={}", seller.getEmail());
                }
            }
        }
        
        //가게 사진 삭제
        if(sellerInformationDto.getSellerPhotoIds() != null && !sellerInformationDto.getSellerPhotoIds().isEmpty()) {
            for(Long photoId : sellerInformationDto.getSellerPhotoIds()) {
                SellerPhoto photoDelete = sellerPhotoRepository.findById(photoId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않은 가게 사진입니다."));

                if(!photoDelete.getSeller().getId().equals(seller.getId())) {
                    throw new IllegalArgumentException("가게 사진에 대한 권한이 없습니다.");
                }

                sellerPhotoRepository.delete(photoDelete);
                seller.getSellerPhotos().remove(photoDelete);
                log.info("가게 사진이 삭제되었습니다 :{}",photoId);
            }
        }
        
        //공지 사진 삭제
        if(sellerInformationDto.getNotificationPhotoIds() != null && !sellerInformationDto.getNotificationPhotoIds().isEmpty()) {
            for(Long photoId : sellerInformationDto.getNotificationPhotoIds()) {
                NotificationPhoto photoDelete = notificationPhotoRepositry.findById(photoId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않은 공지 사진입니다."));

                if(!photoDelete.getSeller().getId().equals(seller.getId())) {
                    throw new IllegalArgumentException("공지 사진에 대한 권한이 없습니다.");
                }

                notificationPhotoRepositry.delete(photoDelete);
                seller.getNotificationPhotos().remove(photoDelete);
                log.info("공지 사진이 삭제되었습니다 :{}",photoId);
            }
        }

        //가게 사진 업로드 + 등록
        if(storeImage != null && !storeImage.isEmpty()) {
            int currentStorePhotoCount = seller.getSellerPhotos().size();
            if(currentStorePhotoCount + storeImage.size() > 5) {
                throw new IllegalArgumentException("가게 사진은 최대 5장 까지 등록할 수 있습니다.");
            }

            List<SellerPhoto> storeAllPhotoUrls = uploadAndCreatePhotos(
                storeImage,
                seller,
                5,
                url -> SellerPhoto.builder()
                        .photoUrl(url)
                        .seller(seller)
                        .build()
            );
            seller.getSellerPhotos().addAll(storeAllPhotoUrls);
        }

        //공지 사진 업로드
        if(notificationImage != null && !notificationImage.isEmpty()) {
            int currentNotificationPhotoCount = seller.getNotificationPhotos().size();
            if(currentNotificationPhotoCount + notificationImage.size() > 5) {
                throw new IllegalArgumentException("공지 사진은 최대 5장 까지 등록할 수 있습니다.");
            }

            List<NotificationPhoto> notificationAllPhotoUrls = uploadAndCreatePhotos(
                    notificationImage,
                    seller,
                    5,
                    url -> NotificationPhoto.builder()
                            .photoUrl(url)
                            .seller(seller)
                            .build()
            );
            seller.getNotificationPhotos().addAll(notificationAllPhotoUrls);
        }

        Seller updatedSeller = sellerRepository.save(seller);

        // Elasticsearch에 가게 정보 인덱싱 업데이트
        sellerIndexService.indexSeller(updatedSeller);

        return updatedSeller;
    }

    private <T> List<T> uploadAndCreatePhotos(
            List<MultipartFile> files,
            Seller seller,
            int maxCount,
            Function<String, T> entityCreator
    ) {
        if(files != null && !files.isEmpty()) {
            if(files.size() > maxCount) {
                throw new IllegalArgumentException("사진은 최대 5장 까지 업로드 할 수 있습니다.");
            }
        }

        List<String> photoUrls = new ArrayList<>();
        try {
            photoUrls = awsS3UploadService.uploadFiles(files);
            log.info("사진 업로드 완료 : {}", photoUrls);
        } catch (Exception e) {
            log.info("사진 업로드 실패 : {}", e.getMessage());
            throw new RuntimeException("사진 업로드 실패했습니다.");
        }

        return photoUrls.stream()
                .map(entityCreator)
                .collect(Collectors.toList());
    }


    public SellerInformationResponseDto  getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게 정보를 찾지 못했습니다."));

        return SellerInformationResponseDto.toDto(seller);
    }

    // 비밀번호 정규식 검사
    private void checkPasswordStrength(String password) {
        if (PASSWORD_PATTERN.matcher(password).matches()) {
            return;
        }
        log.info("비밀번호 정책 미달");
        throw new IllegalArgumentException("비밀번호 최소 8자에 영어, 숫자, 특수문자를 포함해야 합니다.");
    }

    public SellerInforResDto getUserDetailsBySellerId(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));
        return new SellerInforResDto(seller.getId(), seller.getStoreName());
    }

    // 주문번호 기반 주문 상세 조회 
    public SellerOrderDetailResponseDto getOrderDetailByOrderNumber(Long sellerId, String orderNumber) {
        // 판매자 존재 확인
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        // 주문번호로 주문 조회
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다."));

        // 해당 판매자의 주문인지 확인
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 주문에 접근할 권한이 없습니다.");
        }

        // 기존 getOrderDetail 로직 재사용
        return buildOrderDetailResponse(order);
    }

    // 판매자용 주문 상세 조회 id 기반
    public SellerOrderDetailResponseDto getOrderDetail(Long sellerId, Long orderId) {
        // 판매자 존재 확인
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        // 주문 존재 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 해당 판매자의 주문인지 확인
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 주문에 접근할 권한이 없습니다.");
        }

        return buildOrderDetailResponse(order);
    }

    // 공통 응답 생성 로직 (기존 getOrderDetail 메서드에서 분리)
    private SellerOrderDetailResponseDto buildOrderDetailResponse(Order order) {
        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        // 주문 아이템 DTO 생성
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem item : orderItems) {
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

        // 결제 정보 조회
        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        String paymentStatus = paymentOpt.map(payment ->
                payment.isPaySuccessYN() ? "결제완료" : "결제실패").orElse("결제정보없음");

        return SellerOrderDetailResponseDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getCustomer().getPhone())
                .orderDate(order.getCreatedAt())
                .orderItems(orderItemDtos)
                .totalAmount(order.getTotalAmount())
                .usedPoints(order.getUsedPoints())
                .deliveryFee(order.getDeliveryFee())
                .isDelivery(order.getIsDelivery())
                .deliveryAddress(order.getDeliveryAddress())
                .memo(order.getMemo())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(paymentStatus)
                .build();
    }
    // 판매자용 주문 목록 조회 (페이징)
    public SellerOrderPaginationDto getOrderList(Long sellerId, int page, int size) {
        // 판매자 존재 확인
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        // 페이지네이션 설정 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findBySellerIdWithPagination(sellerId, pageable);

        List<SellerOrderListResponseDto> orderListDtos = new ArrayList<>();

        for (Order order : orderPage.getContent()) {
            // 주문 상품명들 조회
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            List<String> productNames = orderItems.stream()
                    .map(item -> item.getProduct().getName())
                    .collect(Collectors.toList());

            // 대표 상품 사진 URL
            String photoUrl = null;
            if (!orderItems.isEmpty()) {
                Product firstProduct = orderItems.get(0).getProduct();
                Optional<ProductPhoto> firstPhoto = productPhotoRepository.findTopByProductOrderById(firstProduct);
                photoUrl = firstPhoto.map(ProductPhoto::getPhotoUrl).orElse(null);
            }

            SellerOrderListResponseDto dto = SellerOrderListResponseDto.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .customerName(order.getCustomer().getName())
                    .totalAmount(order.getTotalAmount())
                    .orderDate(order.getCreatedAt())
                    .orderItems(productNames)
                    .photoUrl(photoUrl)
                    .orderStatus(order.getOrderStatus())
                    .isDelivery(order.getIsDelivery())
                    .build();

            orderListDtos.add(dto);
        }

        return SellerOrderPaginationDto.builder()
                .orders(orderListDtos)
                .hasNext(orderPage.hasNext())
                .totalElements(orderPage.getTotalElements())
                .build();
    }

    // 판매자용 환불 상세 조회
    public SellerRefundDetailResponseDto getRefundDetail(Long sellerId, Long refundId) {
        // 판매자 존재 확인
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        // 환불 신청 존재 확인
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환불 신청입니다."));

        // 결제 정보 조회
        Payment payment = refund.getPayment();
        if (payment == null) {
            throw new IllegalArgumentException("환불 신청에 대한 결제 정보가 없습니다.");
        }

        // 주문 정보 조회
        Order order = payment.getOrder();
        if (order == null) {
            throw new IllegalArgumentException("환불 신청에 대한 주문 정보가 없습니다.");
        }

        // 해당 판매자의 주문인지 확인
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("해당 환불 신청에 접근할 권한이 없습니다.");
        }

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        // 주문 아이템 DTO 생성
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem item : orderItems) {
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

        // 환불 사진 URL 목록 조회
        List<String> refundPhotoUrls = refund.getRefundPhoto().stream()
                .map(RefundPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        // 결제 상태 확인
        String paymentStatus = payment.isPaySuccessYN() ? "결제완료" : "결제실패";

        return SellerRefundDetailResponseDto.builder()
                .refundId(refund.getId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getCustomer().getPhone())
                .orderDate(order.getCreatedAt())
                .refundRequestDate(refund.getRequestedAt())
                .refundProcessedDate(refund.getProcessedAt())
                .orderItems(orderItemDtos)
                .totalAmount(order.getTotalAmount())
                .usedPoints(order.getUsedPoints())
                .deliveryFee(order.getDeliveryFee())
                .isDelivery(order.getIsDelivery())
                .deliveryAddress(order.getDeliveryAddress())
                .orderMemo(order.getMemo())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(paymentStatus)
                .refundType(refund.getRefundType())
                .refundTitle(refund.getTitle())
                .refundContent(refund.getContent())
                .refundStatus(refund.getStatus())
                .refundPhotos(refundPhotoUrls)
                .build();
    }

    public void updateSellerStatus(String email, Character isOpen) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        // isOpen 상태 업데이트 (Seller 엔티티에 메서드 추가 필요)
        seller.updateIsOpen(isOpen);
        sellerRepository.save(seller);

        log.info("판매자 영업 상태 변경: {} -> {}", email, isOpen);
    }
}
