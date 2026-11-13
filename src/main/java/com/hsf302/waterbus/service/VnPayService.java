package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.VnPayResult;
import java.util.Map;

public interface VnPayService {
    String createPaymentUrl(String bookingCode, String clientIp, String bankCode);

    default String createPaymentUrl(String bookingCode, String clientIp) {
        return createPaymentUrl(bookingCode, clientIp, null);
    }

    VnPayResult handleReturn(Map<String, String> params);
}
