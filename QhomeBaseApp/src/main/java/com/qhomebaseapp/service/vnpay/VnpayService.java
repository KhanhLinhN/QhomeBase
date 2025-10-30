package com.qhomebaseapp.service.vnpay;

import com.qhomebaseapp.config.VnpayProperties;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayProperties props;

    public String createPaymentUrl(Long orderId, String orderInfo, BigDecimal amountVnd, String ipAddress) {
        try {
            long amount = amountVnd.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", props.getVersion());
            params.put("vnp_Command", props.getCommand());
            params.put("vnp_TmnCode", props.getTmnCode());
            params.put("vnp_Amount", String.valueOf(amount));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", String.valueOf(orderId));
            params.put("vnp_OrderInfo", orderInfo);
            params.put("vnp_OrderType", "billpayment");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", props.getReturnUrl());
            params.put("vnp_IpAddr", ipAddress);
            params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            String hashData = buildHashData(params);
            String secureHash = hmacSHA512(props.getHashSecret(), hashData);
            String query = buildQuery(params);

            String paymentUrl = props.getVnpUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

            log.info("LOCAL HASHDATA (EXACT): {}", hashData);
            log.info("LOCAL SECUREHASH: {}", secureHash);

            return paymentUrl;
        } catch (Exception e) {
            log.error("Lỗi tạo URL thanh toán VNPAY", e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPAY", e);
        }
    }

    public boolean validateReturn(Map<String, String> params) {
        if (params == null || params.isEmpty()) return false;

        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) return false;

        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");

        String hashData = buildHashData(sorted);
        String calculated = hmacSHA512(props.getHashSecret(), hashData);

        log.info("VALIDATION HASHDATA (EXACT): {}", hashData);
        log.info("CALCULATED HASH: {}", calculated);
        log.info("RECEIVED HASH:   {}", vnpSecureHash);

        boolean match = calculated.equals(vnpSecureHash);
        log.info("MATCH: {}", match);

        return match && "00".equals(params.get("vnp_ResponseCode"));
    }
    private String buildHashData(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            try {
                parts.add(e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                parts.add(e.getKey() + "=" + e.getValue());
            }
        }
        return String.join("&", parts);
    }

    private String buildQuery(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            try {
                parts.add(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                parts.add(e.getKey() + "=" + e.getValue());
            }
        }
        return String.join("&", parts);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
            //  .replace("+", "%20") ở sandbox!
        } catch (Exception e) {
            return value;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(spec);
            return DatatypeConverter.printHexBinary(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}