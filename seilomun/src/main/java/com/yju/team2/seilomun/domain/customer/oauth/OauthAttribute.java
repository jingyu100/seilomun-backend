package com.yju.team2.seilomun.domain.customer.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OauthAttribute {

//    private String name;
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
        return null;
    }

    private static OauthAttribute ofkakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = kakaoAccount.get("email") != null ? kakaoAccount.get("email").toString() : "";
        String nickname = profile.get("nickname") != null ? profile.get("nickname").toString() : "익명";
        String profileImage = profile.get("profile_image_url") != null ? profile.get("profile_image_url").toString() : "";

        return new OauthAttribute(email, nickname, profileImage);
    }
}
