package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.config.VnPayConfig;
import com.hsf302.waterbus.dto.VnPayResult;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Payment;
import com.hsf302.waterbus.repository.PaymentRepository;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
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

// Module: BE4 - Đặt vé & Thanh toán. Cổng VNPay Sandbox (phiên bản API 2.1.0).
// Luồng: createPaymentUrl tạo link -> khách trả tiền trên trang VNPay -> VNPay trả khách về
// /payment/vnpay-return kèm kết quả -> handleReturn kiểm tra rồi ghi nhận.
@Service
public class VnPayServiceImpl implements VnPayService {

    // VNPay dùng ngày giờ dạng 20261109143005 (năm tháng ngày giờ phút giây, 14 chữ số)
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // Chỉ cho chọn sẵn 2 phương thức: VNPAYQR = màn hình mã QR, VNBANK = thẻ ATM / tài khoản ngân hàng nội địa
    private static final Set<String> ALLOWED_BANK_CODES = Set.of("VNPAYQR", "VNBANK");

    private final VnPayConfig config;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    // Constructor injection: Spring tự đưa các bean vào
    public VnPayServiceImpl(VnPayConfig config, BookingService bookingService,
                            PaymentService paymentService, PaymentRepository paymentRepository) {
        this.config = config;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String createPaymentUrl(String bookingCode, String clientIp, String bankCode) {
        if (!config.isConfigured()) {
            throw new IllegalArgumentException("VNPay chưa được cấu hình trên máy chủ. Vui lòng chọn Thanh toán mô phỏng.");
        }
        Booking booking = bookingService.getBookingByCode(bookingCode);
        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé.");
        }
        // Dùng chung luật với payMock: đơn PENDING, còn hạn, chuyến còn SCHEDULED
        String reason = paymentService.getCannotPayReason(booking.getId());
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Đơn 0 đồng không cần thanh toán qua VNPay.");
        }

        // 13 tham số VNPay yêu cầu
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", toVnPayAmount(booking.getTotalAmount())); // VNPay tính tiền x 100
        params.put("vnp_CurrCode", "VND");
        // Mã giao dịch phải khác nhau mỗi lần bấm -> mã đơn + 14 số thời gian (VD BK12345678 + 20261109143005)
        params.put("vnp_TxnRef", booking.getBookingCode() + now.format(VNPAY_TIME));
        params.put("vnp_OrderInfo", "Thanh toan don " + booking.getBookingCode()); // VNPay yêu cầu không dấu
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", toIpv4(clientIp));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", booking.getExpiresAt().format(VNPAY_TIME)); // hết giữ chỗ thì link cũng hết hạn
        // Tham số thứ 14 (không bắt buộc): chọn sẵn phương thức. Phải kiểm null TRƯỚC,
        // vì Set.of(...).contains(null) ném NullPointerException. Mã lạ/rỗng -> không gửi.
        if (bankCode != null && ALLOWED_BANK_CODES.contains(bankCode)) {
            params.put("vnp_BankCode", bankCode);
        }

        // Ký dữ liệu để VNPay biết link do chính mình tạo, không bị ai sửa giữa đường
        String query = buildQuery(params);
        return config.getPayUrl() + "?" + query + "&vnp_SecureHash=" + hmacSHA512(config.getHashSecret(), query);
    }

    // KHÔNG đặt @Transactional: mỗi bước ghi (confirmPaid, lưu payment) tự có giao dịch riêng.
    // Nhờ vậy confirmPaid báo lỗi thì vẫn lưu được payment ở bước dự phòng bên dưới.
    @Override
    public VnPayResult handleReturn(Map<String, String> params) {
        // 1. Chữ ký: bỏ 2 tham số chữ ký ra, tự ký lại phần còn lại, phải trùng với chữ ký VNPay gửi
        String receivedHash = params.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        if (!config.isConfigured() || receivedHash == null
                || !hmacSHA512(config.getHashSecret(), buildQuery(fields)).equalsIgnoreCase(receivedHash)) {
            // Có thể là ai đó tự sửa URL -> không tin, không đụng DB
            return new VnPayResult(null, false, "Dữ liệu thanh toán trả về không hợp lệ.");
        }

        // 2. Mã đơn = vnp_TxnRef bỏ 14 số thời gian ở cuối
        String txnRef = params.get("vnp_TxnRef");
        Booking booking = null;
        if (txnRef != null && txnRef.length() > 14) {
            booking = bookingService.getBookingByCode(txnRef.substring(0, txnRef.length() - 14));
        }
        if (booking == null) {
            return new VnPayResult(null, false, "Không tìm thấy đơn đặt vé của giao dịch này.");
        }
        String bookingCode = booking.getBookingCode();

        // 3. Số tiền VNPay đã trừ phải đúng bằng tổng tiền của đơn (x 100)
        if (!toVnPayAmount(booking.getTotalAmount()).equals(params.get("vnp_Amount"))) {
            return new VnPayResult(bookingCode, false, "Số tiền thanh toán không khớp với đơn.");
        }

        // 4. Kết quả giao dịch: "00" là thành công, "24" là khách bấm Huỷ trên trang VNPay
        String responseCode = params.get("vnp_ResponseCode");
        if (!"00".equals(responseCode) || !"00".equals(params.get("vnp_TransactionStatus"))) {
            if ("24".equals(responseCode)) {
                return new VnPayResult(bookingCode, false, "Bạn đã huỷ thanh toán VNPay.");
            }
            return new VnPayResult(bookingCode, false,
                    "Thanh toán VNPay không thành công (mã " + responseCode + "). Vui lòng thử lại.");
        }

        // 5. Đơn đã PAID (khách bấm F5 trang kết quả) -> đã ghi nhận lần trước rồi, không ghi thêm
        if ("PAID".equals(booking.getStatus())) {
            return new VnPayResult(bookingCode, true, "Đơn đã được thanh toán.");
        }

        String transactionNo = params.get("vnp_TransactionNo"); // mã giao dịch phía VNPay
        // 6. VNPay đã trừ tiền nhưng đơn không còn thanh toán được (hết hạn giữ chỗ / chuyến bị huỷ)
        if (paymentService.getCannotPayReason(booking.getId()) != null) {
            return recordPaidButInvalid(booking, transactionNo);
        }

        // 7. Mọi thứ hợp lệ -> ghi nhận giống payMock: payment SUCCESS, đơn PAID, vé VALID
        try {
            paymentService.confirmPaid(booking.getId(), "VNPAY", transactionNo);
            return new VnPayResult(bookingCode, true, "Thanh toán VNPay thành công! Vé của bạn đã sẵn sàng.");
        } catch (IllegalArgumentException ex) {
            // Hiếm: đơn vừa hết hạn đúng giữa bước 6 và 7 -> xử lý như bước 6 để không mất dấu tiền khách đã trả
            return recordPaidButInvalid(booking, transactionNo);
        }
    }

    // Bước 6: chỉ lưu payment SUCCESS làm bằng chứng khách đã trả tiền, KHÔNG đổi trạng thái đơn/vé.
    // Nhân viên xem payment này để xử lý cho khách.
    private VnPayResult recordPaidButInvalid(Booking booking, String transactionNo) {
        // DB chỉ cho mỗi đơn 1 payment -> đã có thì dùng lại
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setCreatedAt(LocalDateTime.now());
        }
        // Đã lưu SUCCESS rồi (khách bấm F5) -> giữ nguyên, không ghi đè
        if (!"SUCCESS".equals(payment.getStatus())) {
            payment.setMethod("VNPAY");
            payment.setAmount(booking.getTotalAmount());
            payment.setStatus("SUCCESS");
            payment.setTransactionRef(transactionNo);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        return new VnPayResult(booking.getBookingCode(), false,
                "VNPay đã trừ tiền nhưng đơn không còn hiệu lực. Vui lòng liên hệ nhân viên kèm mã đơn "
                        + booking.getBookingCode() + ".");
    }

    // Tiền gửi VNPay = tổng tiền x 100, dạng số nguyên (VD 27000 đ -> "2700000")
    private String toVnPayAmount(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
    }

    // VNPay chỉ nhận IPv4. Chạy localhost thường ra IPv6 "0:0:0:0:0:0:0:1" (có dấu ":") -> đổi thành 127.0.0.1
    private String toIpv4(String ip) {
        if (ip == null || ip.isBlank() || ip.contains(":")) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * Ghép tham số thành chuỗi "a=1&b=2" theo đúng cách VNPay ký:
     * tên tham số sắp A -> Z, bỏ giá trị rỗng, giá trị được mã hoá URL (US_ASCII, khoảng trắng thành "+").
     * public static để test dùng lại khi tự tạo dữ liệu VNPay trả về.
     */
    public static String buildQuery(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names); // sắp tên tham số A -> Z
        StringBuilder query = new StringBuilder();
        for (String name : names) {
            String value = params.get(name);
            if (value == null || value.isEmpty()) {
                continue; // VNPay không ký tham số rỗng
            }
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
        }
        return query.toString();
    }

    /**
     * Ký dữ liệu bằng HMAC-SHA512 với chuỗi bí mật, trả về chuỗi hex (chữ thường).
     * Cùng dữ liệu + cùng bí mật -> luôn ra cùng chữ ký; sửa 1 ký tự là chữ ký khác hẳn.
     */
    public static String hmacSHA512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b)); // mỗi byte -> 2 ký tự hex
            }
            return hex.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay.", ex);
        }
    }
}
