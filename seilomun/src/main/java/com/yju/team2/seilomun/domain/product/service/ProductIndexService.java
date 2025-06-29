package com.yju.team2.seilomun.domain.product.service;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductDocument;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import com.yju.team2.seilomun.domain.product.repository.ProductPhotoRepository;
import com.yju.team2.seilomun.domain.product.repository.ProductRepository;
import com.yju.team2.seilomun.domain.search.repository.ProductSearchRepository;
import com.yju.team2.seilomun.domain.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final ProductSearchService productSearchService;

    // 새로운 상품 정보를 인덱싱
    @Transactional(readOnly = true)
    public void indexProduct(Product product) {
        try {
            ProductDocument productDocument = ProductDocument.from(product);

            // 상품 썸네일 이미지 URL 설정 (첫 번째 사진)
            Optional<ProductPhoto> firstPhoto = productPhotoRepository.findTopByProductOrderById(product);
            if (firstPhoto.isPresent()) {
                productDocument.setThumbnailUrl(firstPhoto.get().getPhotoUrl());
            }
            productSearchService.indexProductDocument(productDocument);
            log.info("상품 정보 인덱싱 완료: id={}, name={}", product.getId(), product.getName());
        } catch (Exception e) {
            log.error("상품 정보 인덱싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 상품 정보 인덱스 삭제
    public void deleteProduct(Long productId) {
        try {
            productSearchService.deleteProductDocument(productId.toString());
            log.info("상품 정보 인덱스 삭제 완료: id={}", productId);
        } catch (Exception e) {
            log.error("상품 정보 인덱스 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 매일 새벽 2시 30분에 모든 상품 정보 인덱스 업데이트
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional(readOnly = true)
    public void reindexAllProducts() {
        log.info("모든 상품 정보 재인덱싱 작업 시작");
        try {
            List<Product> allProducts = productRepository.findAll();
            int count = 0;

            for (Product product : allProducts) {
                // 정상 상품 및 유통기한이 지나지 않은 상품만 인덱싱
                LocalDateTime now = LocalDateTime.now();
                if (product.getStatus() == '1' || product.getStatus() == 'T') {
                    ProductDocument productDocument = ProductDocument.from(product);

                    // 썸네일 URL 설정
                    Optional<ProductPhoto> firstPhoto = productPhotoRepository.findTopByProductOrderById(product);
                    if (firstPhoto.isPresent()) {
                        productDocument.setThumbnailUrl(firstPhoto.get().getPhotoUrl());
                    }

                    productSearchRepository.save(productDocument);
                    count++;
                }
            }

            log.info("모든 상품 정보 재인덱싱 완료: 총 {}개 처리됨", count);
        } catch (Exception e) {
            log.error("상품 정보 재인덱싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 개별 상품 상태 변경 시 호출되는 메서드 (ProductService에서 사용)
    public void updateProductIndex(Product product) {
        try {
            if (product.getStatus() == '1' || product.getStatus() == 'T') {
                // 정상 상품이거나 임박특가 상품이면 인덱스 업데이트
                indexProduct(product);
            } else {
                // 그 외 상태(판매중단, 유통기한만료 등)면 인덱스에서 제거
                deleteProduct(product.getId());
            }
        } catch (Exception e) {
            log.error("상품 인덱스 업데이트 중 오류 발생: productId={}", product.getId(), e);
        }
    }

}