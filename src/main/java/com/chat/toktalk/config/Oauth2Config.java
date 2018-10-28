package com.chat.toktalk.config;

import org.springframework.context.annotation.Configuration;

//@Configuration
//@EnableOAuth2Client
public class Oauth2Config {

////    @Autowired
////    @Qualifier("oauth2ClientContext")
//    private final OAuth2ClientContext oAuth2ClientContext;
//    private final Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
//
//    public Oauth2Config(OAuth2ClientContext oAuth2ClientContext, Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler) {
//        this.oAuth2ClientContext = oAuth2ClientContext;
//        this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
//    }
//
//    //필터 등록
//    @Bean
//    public Filter googleFilter(){
//        OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter("/login/google");
//        filter.setRestTemplate(new OAuth2RestTemplate(googleClient(),oAuth2ClientContext));
//        filter.setTokenServices(new UserInfoTokenServices(googleResource().getUserInfoUri(),googleClient().getClientId()));
//        filter.setAuthenticationSuccessHandler(oauth2AuthenticationSuccessHandler);
//
//        return filter;
//    }
//
//    //프로퍼티 빈 설정
//    //clientID,secret,accessTokenUri..등의 속성포함됨
//    @Bean
//    @ConfigurationProperties("google.client")
//    public OAuth2ProtectedResourceDetails googleClient(){
//        return new AuthorizationCodeResourceDetails();
//    }
//
//    @Bean
//    @ConfigurationProperties("google.resource")
//    public ResourceServerProperties googleResource(){
//        return new ResourceServerProperties();
//    }
//
//    //리다이렉션 처리를 위한 필터 등록
//    //스프링 Security Filter이전에 등록.
//    @Bean
//    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
//
//        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//        registrationBean.setFilter(filter);
//        registrationBean.setOrder(-100);
//        return registrationBean;
//    }
}