package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.config.VnPayConfig;
import com.hsf302.waterbus.dto.VnPayResult;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.repository.PaymentRepository;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
import com.hsf302.waterbus.service.RefundService;
import com.hsf302.waterbus.service.VnPayService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class VnPayServiceImpl implements VnPayService {

    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Set<String> ALLOWED_BANK_CODES = Set.of("VNPAYQR", "VNBANK");

    private final VnPayConfig config;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    private static final String INVALID_PAYMENT_REFUND_REASON = "Thanh toán sau khi đơn hết hạn";

    public VnPayServiceImpl(VnPayConfig config, BookingService bookingService,
                            PaymentService paymentService, PaymentRepository paymentRepository,
                            RefundService refundService) {
        this.config = config;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.refundService = refundService;
    }

    @Override
    public String createPaymentUrl(String bookingCode, String clientIp, String bankCode) {
        if (!config.isConfigured()) {
            throw new IllegalArgumentException("Hiện chưa thanh toán được qua VNPay. Vui lòng thử lại sau hoặc liên hệ nhân viên.");
        }
        Booking booking = bookingService.getBookingByCode(bookingCode);
        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé.");
        }
        String reason = paymentService.getCannotPayReason(booking.getId());
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Đơn 0 đồng không cần thanh toán qua VNPay.");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", toVnPayAmount(booking.getTotalAmount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", booking.getBookingCode() + now.format(VNPAY_TIME));
        params.put("vnp_OrderInfo", "Thanh toan don " + booking.getBookingCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", toIpv4(clientIp));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", booking.getExpiresAt().format(VNPAY_TIME));
        if (bankCode != null && ALLOWED_BANK_CODES.contains(bankCode)) {
            params.put("vnp_BankCode", bankCode);
        }

        String query = buildQuery(params);
        return config.getPayUrl() + "?" + query + "&vnp_SecureHash=" + hmacSHA512(config.getHashSecret(), query);
    }

    @Override
    public VnPayResult handleReturn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        if (!config.isConfigured() || receivedHash == null
                || !hmacSHA512(config.getHashSecret(), buildQuery(fields)).equalsIgnoreCase(receivedHash)) {
            return new VnPayResult(null, false, "Dữ liệu thanh toán trả về không hợp lệ.");
        }

        String txnRef = params.get("vnp_TxnRef");
        Booking booking = null;
        if (txnRef != null && txnRef.length() > 14) {
            booking = bookingService.getBookingByCode(txnRef.substring(0, txnRef.length() - 14));
        }
        if (booking == null) {
            return new VnPayResult(null, false, "Không tìm thấy đơn đặt vé của giao dịch này.");
        }
        String bookingCode = booking.getBookingCode();

        if (!toVnPayAmount(booking.getTotalAmount()).equals(params.get("vnp_Amount"))) {
            return new VnPayResult(bookingCode, false, "Số tiền thanh toán không khớp với đơn.");
        }

        String responseCode = params.get("vnp_ResponseCode");
        if (!"00".equals(responseCode) || !"00".equals(params.get("vnp_TransactionStatus"))) {
            if ("24".equals(responseCode)) {
                return new VnPayResult(bookingCode, false, "Bạn đã huỷ thanh toán VNPay.");
            }
            return new VnPayResult(bookingCode, false,
                    "Thanh toán VNPay không thành công (mã " + responseCode + "). Vui lòng thử lại.");
        }

        if ("PAID".equals(booking.getStatus())) {
            return new VnPayResult(bookingCode, true, "Đơn đã được thanh toán.");
        }

        String transactionNo = params.get("vnp_TransactionNo");
        if (paymentService.getCannotPayReason(booking.getId()) != null) {
            return recordPaidButInvalid(booking, transactionNo);
        }

        try {
            paymentService.confirmPaid(booking.getId(), "VNPAY", transactionNo);
            return new VnPayResult(bookingCode, true, "Thanh toán VNPay thành công! Vé của bạn đã sẵn sàng.");
        } catch (IllegalArgumentException ex) {
            return recordPaidButInvalid(booking, transactionNo);
        }
    }

    private VnPayResult recordPaidButInvalid(Booking booking, String transactionNo) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setCreatedAt(LocalDateTime.now());
        }
        if (!"SUCCESS".equals(payment.getStatus())) {
            payment.setMethod("VNPAY");
            payment.setAmount(booking.getTotalAmount());
            payment.setStatus("SUCCESS");
            payment.setTransactionRef(transactionNo);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        refundService.createForInvalidPayment(booking.getId(), INVALID_PAYMENT_REFUND_REASON);
        return new VnPayResult(booking.getBookingCode(), false,
                "VNPay đã trừ tiền nhưng đơn không còn hiệu lực. Vui lòng liên hệ nhân viên kèm mã đơn "
                        + booking.getBookingCode() + ".");
    }

    private String toVnPayAmount(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
    }

    private String toIpv4(String ip) {
        if (ip == null || ip.isBlank() || ip.contains(":")) {
            return "127.0.0.1";
        }
        return ip;
    }

    public static String buildQuery(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        StringBuilder query = new StringBuilder();
        for (String name : names) {
            String value = params.get(name);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
        }
        return query.toString();
    }

    public static String hmacSHA512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay.", ex);
        }
    }
}
