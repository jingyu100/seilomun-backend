package com.yju.team2.seilomun.util;


import jakarta.annotation.PostConstruct;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsUtil {

    @Value("${coolsms.api.key}")
    private String apikey;

    @Value("${coolsms.api.secret}")
    private String apisecret;

    @Value("${coolsms.sender.phone}")
    private String sendPhone;
    private DefaultMessageService messageService;

    @PostConstruct
    private void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apikey,apisecret,"https://api.coolsms.co.kr");
    }

    public SingleMessageSentResponse sendOne(String to, String verificationCode) {
        try {
            Message message = new Message();
            message.setFrom(sendPhone);
            message.setTo(to);
            message.setText("[Seilomun] 인증번호를 입력해주세요\n" + verificationCode);
            SingleMessageSentResponse singleMessageSentResponse = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            return singleMessageSentResponse;
        }catch (Exception e) {
            throw new RuntimeException("SMS 전송 실패",e);
        }
    }
}
