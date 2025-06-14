package com.yju.team2.seilomun.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalUserProfileDto {

    private MultipartFile profileImage;
    private String profileImageUrl;
}
