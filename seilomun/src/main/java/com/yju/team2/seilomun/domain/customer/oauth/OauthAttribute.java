package com.yju.team2.seilomun.domain.customer.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class OauthAttribute {

    private String name;
    private String birthday;
    private String email;
    private String nickname;
    private String profile;


    public static OauthAttribute of(String registId, Map<String, Object> attributes) {
        if ("kakao".equals(registId)) {
            return ofkakao(attributes);
        } else if ("naver".equals(registId)) {
            return ofnaver(attributes);
        }
        return null;
    }

    private static OauthAttribute ofnaver(Map<String, Object> attributes) {
        // 네이버는 response 키 내부에 사용자 정보가 포함됨
        Map<String,Object> response = (Map<String, Object>) attributes.get("response");

        String providerId = response.get("id").toString();
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String nickName = "naver_"+providerId;
        String birthday = (String) response.get("birthday");
        String profile = "";

        log.info("네이버 Oauth 정보 - 이메일 : {}, 닉네임 : {}, 프로필 : {} , 생일 : {} ", email, name, nickName, birthday);

        return new OauthAttribute(name,birthday,email, nickName, profile);
    }

    // 카카오는 kakao_account 에서 이메일을 제공해주고
    // profile에서 닉네임이랑 프로필사진을 제공해줌
    private static OauthAttribute ofkakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String name = profile.get("nickname").toString();
        String birthday = "";

        String providerId = attributes.get("id").toString();
        // 카카오 이메일
        String email = kakaoAccount.get("email").toString();
        // 카카오 닉네임
        String nickname = "kakao_"+providerId;
        // 카카오 이미지사진
        String profileImage = profile.get("profile_image_url").toString();

        log.info("카카오 OAuth 정보 - email: {}, nickname: {}, profileImage: {}", email, nickname, profileImage);


        return new OauthAttribute(name,birthday,email, nickname, profileImage);
    }
}
