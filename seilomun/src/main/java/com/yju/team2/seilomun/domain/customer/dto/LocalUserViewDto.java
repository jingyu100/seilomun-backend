package com.yju.team2.seilomun.domain.customer.dto;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocalUserViewDto {

    private String name;
    private String email;
    private String nickname;
    private String phone;
    private Character gender;
    private String birthday;
    private String profileImageUrl;

    public static LocalUserViewDto from(Customer customer) {
        return new LocalUserViewDto(
                customer.getName(),
                customer.getEmail(),
                customer.getNickname(),
                customer.getPhone(),
                customer.getGender(),
                customer.getBirthDate(),
                customer.getProfileImageUrl()
        );
    }
}
