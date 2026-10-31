package com.ecommerce.platform.service;

import com.ecommerce.platform.service.impl.VnpayServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class VnpayTestFixtures {

    private static final String SECRET = "test-secret";

    private VnpayTestFixtures() {
    }

    static void configure(VnpayServiceImpl service) {
        ReflectionTestUtils.setField(service, "tmnCode", "test-merchant");
        ReflectionTestUtils.setField(service, "hashSecret", SECRET);
    }

    static Map<String, String> signedIpn() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_Amount", "10000000");
        values.put("vnp_ResponseCode", "00");
        values.put("vnp_CurrCode", "VND");
        values.put("vnp_SecureHashType", "HmacSHA512");
        values.put("vnp_TmnCode", "test-merchant");
        values.put("vnp_TransactionStatus", "00");
        values.put("vnp_TxnRef", "42-ref");
        values.put("vnp_SecureHash", signature(values));
        return values;
    }

    static Map<String, String> signedReturn(String responseCode, String transactionStatus) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_Amount", "10000000");
        values.put("vnp_CurrCode", "VND");
        values.put("vnp_ResponseCode", responseCode);
        values.put("vnp_TmnCode", "test-merchant");
        values.put("vnp_TransactionStatus", transactionStatus);
        values.put("vnp_TxnRef", "42-ref");
        values.put("vnp_SecureHash", signature(values));
        return values;
    }

    static String signature(Map<String, String> values) throws Exception {
        String canonical = values.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey())
                        && !"vnp_SecureHashType".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));

        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] bytes = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    static Map<String, String> queryParameters(String url) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            parameters.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return parameters;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
