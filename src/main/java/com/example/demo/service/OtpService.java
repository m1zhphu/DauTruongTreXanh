package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Autowired private JavaMailSender mailSender;

    // Lưu OTP tạm thời: Key = Username, Value = OTP Code
    // (Thực tế nên dùng Redis và set thời gian hết hạn)
    private Map<String, String> otpStorage = new HashMap<>();

    public void generateAndSendOtp(String username, String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(username, otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác nhận bán vật phẩm - Sử Sử Ký");
        message.setText("Mã OTP xác nhận của bạn là: " + otp + "\n\nMã này có hiệu lực trong phiên giao dịch hiện tại.");
        
        mailSender.send(message);
        System.out.println("Sent OTP to " + email + ": " + otp);
    }

    public boolean validateOtp(String username, String inputOtp) {
        String storedOtp = otpStorage.get(username);
        if (storedOtp != null && storedOtp.equals(inputOtp)) {
            otpStorage.remove(username); // Xóa OTP sau khi dùng
            return true;
        }
        return false;
    }
}