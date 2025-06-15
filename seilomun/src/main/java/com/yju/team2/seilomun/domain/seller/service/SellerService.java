package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.auth.service.RefreshTokenService;
import com.yju.team2.seilomun.domain.customer.entity.Customer;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
                .postCode("11111")
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
    public Seller updateSellerInformation(String email, SellerInformationDto sellerInformationDto, List<MultipartFile> storeImage) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자입니다."));

        SellerCategoryEntity sellerCategory = sellerCategoryRepository.findById(sellerInformationDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        seller.updateInformation(sellerInformationDto,sellerCategory);

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

        if(storeImage != null && !storeImage.isEmpty()) {
            uploadStoreImage(seller, storeImage);
        }


        Seller updatedSeller = sellerRepository.save(seller);

        // Elasticsearch에 가게 정보 인덱싱 업데이트
        sellerIndexService.indexSeller(updatedSeller);

        return updatedSeller;
    }

    private void uploadStoreImage(Seller seller, List<MultipartFile> storeImage) {
        long currentImageCount = sellerPhotoRepository.countBySeller(seller);

        if(currentImageCount + storeImage.size() > 5) {
            throw new IllegalArgumentException("매장 이미지는 최대 5장까지만 등록 가능합니다.");
        }

        for(MultipartFile multipartFile : storeImage) {
            if(multipartFile != null && !multipartFile.isEmpty()) {
                try {
                    String photoUrl = awsS3UploadService.uploadFile(multipartFile);

                    SellerPhoto sellerPhoto = new SellerPhoto();
                    sellerPhoto.UpdatePhoto(photoUrl,seller);

                    sellerPhotoRepository.save(sellerPhoto);
                }catch (Exception e) {
                    throw new IllegalArgumentException("매장 이미지 업로드 중 오류가 발생했습니다.");
                }
            }
        }


    }

    public SellerInformationResponseDto getSellerById(Long id) {
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
        return new SellerInforResDto(seller.getId(),seller.getStoreName());
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
