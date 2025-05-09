package com.yju.team2.seilomun.domain.seller.service;

import com.yju.team2.seilomun.domain.search.service.SellerSearchService;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.entity.SellerDocument;
import com.yju.team2.seilomun.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerIndexService {

    private final SellerRepository sellerRepository;
    private final SellerSearchService sellerSearchService;

    // 새로운 가게 정보를 인덱싱
    @Transactional(readOnly = true)
    public void indexSeller(Seller seller) {
        try {
            SellerDocument sellerDocument = SellerDocument.from(seller);
            sellerSearchService.indexSellerDocument(sellerDocument);
            log.info("가게 정보 인덱싱 완료: id={}, name={}", seller.getId(), seller.getStoreName());
        } catch (Exception e) {
            log.error("가게 정보 인덱싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 가게 정보 인덱스 삭제
    public void deleteSeller(Long sellerId) {
        try {
            sellerSearchService.deleteSellerDocument(sellerId.toString());
            log.info("가게 정보 인덱스 삭제 완료: id={}", sellerId);
        } catch (Exception e) {
            log.error("가게 정보 인덱스 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 매일 새벽 2시에 모든 가게 정보 인덱스 업데이트
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(readOnly = true)
    public void reindexAllSellers() {
        log.info("모든 가게 정보 재인덱싱 작업 시작");
        try {
            List<Seller> allSellers = sellerRepository.findAll();
            int count = 0;

            for (Seller seller : allSellers) {
                // 상태가 정상인 가게만 인덱싱
                if (seller.getStatus() == '1') {
                    SellerDocument sellerDocument = SellerDocument.from(seller);
                    sellerSearchService.indexSellerDocument(sellerDocument);
                    count++;
                }
            }

            log.info("모든 가게 정보 재인덱싱 완료: 총 {}개 처리됨", count);
        } catch (Exception e) {
            log.error("가게 정보 재인덱싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}