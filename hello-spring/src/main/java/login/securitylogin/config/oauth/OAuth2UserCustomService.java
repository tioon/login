package login.securitylogin.config.oauth;

import login.securitylogin.domain.User;
import login.securitylogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;


    //리소스 서버 -> 스프링 서버 (Oauth2 인증 후 스프링 서버로 사용자 정보 전달)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest); // ❶ 요청을 바탕으로 유저 정보를 담은 객체 반환

        System.out.println(userRequest.getClientRegistration());
        String provider = userRequest.getClientRegistration().getRegistrationId(); // 제공자 확인(kakao, google)

        //saveOrUpdate(user,provider); // DB 업데이트 작업

        return user;
    }

    // ❷ 유저가 있으면 업데이트, 없으면 유저 생성
    private User saveOrUpdate(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes(); // OAuth2User에서 속성 정보 가지고 옴

        String email;
        String name;

        if (provider.equals("google")) {
            email = (String) oAuth2User.getAttributes().get("email"); // 구글이 제공하는 이메일 정보
            name = (String) oAuth2User.getAttributes().get("name"); // 구글이 제공하는 이름 정보

        } else if (provider.equals("kakao")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            email = (String) oAuth2User.getAttributes().get("id"); // 카카오가 제공하는 이메일 정보
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            name = (String)  oAuth2User.getAttributes().get("id"); // 카카오가 제공하는 이름 정보
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_provider", "Invalid provider: " + provider, null));
        }


        User user = userRepository.findByEmail(email) // 이메일로 사용자 찾음 (있으면 사용자 이름 업데이트, 없으면 새로 생성)
                .map(entity -> entity.update(name))
                .orElse(User.builder()
                        .email(email)
                        .nickname(name)
                        .build());

        return userRepository.save(user); // 레포에 저장 및 업데이트 후 반환
    }
}

