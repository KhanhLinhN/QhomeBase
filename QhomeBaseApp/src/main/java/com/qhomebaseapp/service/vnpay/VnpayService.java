package com.qhomebaseapp.service.vnpay;

import com.qhomebaseapp.config.VnpayProperties;
import jakarta.servlet.http.HttpServletRequest;
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

            // 🔹 Tạo txnRef unique: billId_timestamp
            String txnRef = orderId + "_" + System.currentTimeMillis();
            params.put("vnp_TxnRef", txnRef);

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

            log.info("💳 [VNPAY] Tạo payment URL: orderId={}, amount={}, ip={}", orderId, amountVnd, ipAddress);
            log.info(">>> txnRef={}", txnRef);
            log.info(">>> hashData={}", hashData);
            log.info(">>> secureHash={}", secureHash);

            return paymentUrl;
        } catch (Exception e) {
            log.error("❌ [VNPAY ERROR] Lỗi tạo URL thanh toán", e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPAY", e);
        }
    }

    public boolean validateReturn(Map<String, String> params) {
        if (params == null || params.isEmpty()) return false;

        log.info("[VNPAY RAW PARAMS START]");
        params.forEach((k, v) -> log.info("{} = {}", k, v));
        log.info("[VNPAY RAW PARAMS END]");

        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) return false;

        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        Map<String, String> sorted = new TreeMap<>(copy);
        log.info("[DEBUG] KEYS USED FOR HASH: {}", sorted.keySet());

        String hashData = buildHashData(sorted);
        String calculated = hmacSHA512(props.getHashSecret(), hashData);

        log.info("🔎 hashData={}", hashData);
        log.info("🔎 calculated hash={}", calculated);
        log.info("🔎 received hash={}", vnpSecureHash);

        return calculated.equalsIgnoreCase(vnpSecureHash) && "00".equals(params.get("vnp_ResponseCode"));
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
    public Map<String, String> getVnpayParams(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }
        return fields;
    }
}