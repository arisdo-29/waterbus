package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.AdminTripDTO;
import com.hsf302.waterbus.dto.BookingCheckinDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.service.TicketService;
import com.hsf302.waterbus.service.TripService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/checkin")
public class AdminCheckinController {

    private final TicketService ticketService;
    private final TripService tripService;

    public AdminCheckinController(TicketService ticketService, TripService tripService) {
        this.ticketService = ticketService;
        this.tripService = tripService;
    }

    @GetMapping
    public String index(@RequestParam(name = "tripId", required = false) Integer tripId,
                        @RequestParam(name = "ticketCode", required = false) String ticketCode,
                        @RequestParam(name = "filter", required = false, defaultValue = "all") String filter,
                        @RequestParam(name = "keyword", required = false) String keyword,
                        HttpSession session,
                        Model model) {

        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer staffId = loginAccount != null ? loginAccount.getId() : null;

        List<AdminTripDTO> allTrips = tripService.getTripsForAdmin();
        LocalDate today = LocalDate.now();
        List<AdminTripDTO> tripsToday = allTrips.stream()
                .filter(t -> t.departureTime() != null && t.departureTime().toLocalDate().isEqual(today))
                .sorted(Comparator.comparing(AdminTripDTO::departureTime))
                .toList();

        if (tripsToday.isEmpty()) {
            tripsToday = allTrips.stream()
                    .sorted(Comparator.comparing(AdminTripDTO::departureTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
        model.addAttribute("tripsToday", tripsToday);

        AdminTripDTO selectedTrip = null;
        if (tripId != null) {
            selectedTrip = allTrips.stream().filter(t -> t.id().equals(tripId)).findFirst().orElse(null);
        }
        if (selectedTrip == null && !tripsToday.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            selectedTrip = tripsToday.stream()
                    .filter(t -> t.departureTime() != null && t.departureTime().isAfter(now.minusMinutes(30)))
                    .findFirst()
                    .orElse(tripsToday.get(0));
        }
        model.addAttribute("selectedTrip", selectedTrip);
        model.addAttribute("selectedTripId", selectedTrip != null ? selectedTrip.id() : null);
        model.addAttribute("isTripMismatch", false);

        if (ticketCode != null && !ticketCode.trim().isEmpty()) {
            String cleanCode = ticketCode.trim().toUpperCase();
            model.addAttribute("ticketCode", cleanCode);

            try {
                if (cleanCode.startsWith("BK")) {
                    BookingCheckinDTO bookingCheckin = ticketService.getBookingCheckinInfo(cleanCode, staffId, "MANUAL");
                    model.addAttribute("previewBooking", bookingCheckin);
                    boolean isDifferentTrip = selectedTrip != null && bookingCheckin.getTripId() != null
                            && !bookingCheckin.getTripId().equals(selectedTrip.id());
                    boolean isOutsideWindow = false;
                    if (bookingCheckin.getDepartureTime() != null) {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        java.time.LocalDateTime dep = bookingCheckin.getDepartureTime();
                        if (!now.toLocalDate().isEqual(dep.toLocalDate())
                                || now.isBefore(dep.minusMinutes(60))
                                || now.isAfter(dep.plusMinutes(15))) {
                            isOutsideWindow = true;
                        }
                    }
                    if (isDifferentTrip || isOutsideWindow) {
                        model.addAttribute("isTripMismatch", true);
                        model.addAttribute("mismatchTripCode", bookingCheckin.getTripCode());
                        model.addAttribute("mismatchDepartureTime", bookingCheckin.getDepartureTime());
                    }
                } else {
                    TicketCheckinDTO ticket = ticketService.getTicketInfoWithLog(cleanCode, staffId, "MANUAL");
                    model.addAttribute("previewTicket", ticket);
                    boolean isDifferentTrip = selectedTrip != null && ticket.getTripId() != null
                            && !ticket.getTripId().equals(selectedTrip.id());
                    boolean isOutsideWindow = false;
                    if (ticket.getDepartureTime() != null) {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        java.time.LocalDateTime dep = ticket.getDepartureTime();
                        if (!now.toLocalDate().isEqual(dep.toLocalDate())
                                || now.isBefore(dep.minusMinutes(60))
                                || now.isAfter(dep.plusMinutes(15))) {
                            isOutsideWindow = true;
                        }
                    }
                    if (isDifferentTrip || isOutsideWindow) {
                        model.addAttribute("isTripMismatch", true);
                        model.addAttribute("mismatchTripCode", ticket.getTripCode());
                        model.addAttribute("mismatchDepartureTime", ticket.getDepartureTime());
                    }
                }
            } catch (Exception e) {
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("resultStatus", "error");
                model.addAttribute("resultMessage", e.getMessage());
            }
        }

        if (selectedTrip != null) {
            List<TicketCheckinDTO> manifest = ticketService.getPassengerManifest(selectedTrip.id());
            long totalCount = manifest.size();
            long boardedCount = manifest.stream().filter(TicketCheckinDTO::isUsed).count();
            model.addAttribute("totalPassengers", totalCount);
            model.addAttribute("boardedPassengers", boardedCount);

            List<TicketCheckinDTO> filteredManifest = manifest.stream()
                    .filter(t -> {
                        if ("unboarded".equalsIgnoreCase(filter)) return t.isValid();
                        if ("boarded".equalsIgnoreCase(filter)) return t.isUsed();
                        if ("priority".equalsIgnoreCase(filter)) return Boolean.TRUE.equals(t.getRequiresVerification());
                        return true;
                    })
                    .filter(t -> {
                        if (keyword == null || keyword.trim().isEmpty()) return true;
                        String kw = keyword.trim().toLowerCase();
                        return (t.getPassengerName() != null && t.getPassengerName().toLowerCase().contains(kw))
                                || (t.getPassengerPhone() != null && t.getPassengerPhone().contains(kw))
                                || (t.getSeatCode() != null && t.getSeatCode().toLowerCase().contains(kw))
                                || (t.getTicketCode() != null && t.getTicketCode().toLowerCase().contains(kw));
                    })
                    .toList();

            model.addAttribute("manifest", filteredManifest);
        } else {
            model.addAttribute("manifest", List.of());
            model.addAttribute("totalPassengers", 0);
            model.addAttribute("boardedPassengers", 0);
        }

        model.addAttribute("currentFilter", filter);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");

        return "admin/checkin/index";
    }

    @PostMapping
    public String checkIn(@RequestParam("ticketCode") String ticketCode,
                          @RequestParam(name = "tripId", required = false) Integer tripId,
                          @RequestParam(name = "source", required = false, defaultValue = "MANUAL") String source,
                          @RequestParam(name = "override", required = false, defaultValue = "false") boolean override,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer staffId = loginAccount != null ? loginAccount.getId() : null;

        try {
            TicketCheckinDTO checkedIn = ticketService.checkIn(ticketCode.trim().toUpperCase(), staffId, source, override);
            redirectAttributes.addFlashAttribute("resultStatus", "success");
            redirectAttributes.addFlashAttribute("resultMessage",
                    "Vé [" + checkedIn.getTicketCode() + "] - Ghế " + checkedIn.getSeatCode() + " (" + checkedIn.getPassengerName() + "), mời lên tàu!");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Soát vé thành công! Vé [" + checkedIn.getTicketCode() + "] đã được xác nhận hợp lệ lên tàu.");
            redirectAttributes.addFlashAttribute("checkedInTicket", checkedIn);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultStatus", "error");
            redirectAttributes.addFlashAttribute("resultMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("ticketCode", ticketCode.trim().toUpperCase());
        }

        return "redirect:/admin/checkin" + (tripId != null ? "?tripId=" + tripId : "");
    }

    @PostMapping("/group")
    public String checkInGroup(@RequestParam("ticketCodes") List<String> ticketCodes,
                               @RequestParam(name = "tripId", required = false) Integer tripId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer staffId = loginAccount != null ? loginAccount.getId() : null;

        try {
            List<TicketCheckinDTO> checkedList = ticketService.checkInGroup(ticketCodes, staffId, "GROUP");
            redirectAttributes.addFlashAttribute("resultStatus", "success");
            redirectAttributes.addFlashAttribute("resultMessage",
                    "Đã soát thành công " + checkedList.size() + " vé cho đoàn lên tàu!");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Soát vé cả nhóm thành công! Đã xác nhận " + checkedList.size() + " khách lên tàu.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultStatus", "error");
            redirectAttributes.addFlashAttribute("resultMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/checkin" + (tripId != null ? "?tripId=" + tripId : "");
    }

    @PostMapping("/reject")
    public String rejectPriorityTicket(@RequestParam("ticketCode") String ticketCode,
                                       @RequestParam(name = "reason", required = false, defaultValue = "Không xuất trình được giấy tờ") String reason,
                                       @RequestParam(name = "tripId", required = false) Integer tripId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {

        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer staffId = loginAccount != null ? loginAccount.getId() : null;

        try {
            TicketCheckinDTO rejected = ticketService.rejectPriorityTicket(ticketCode.trim().toUpperCase(), reason, staffId);
            redirectAttributes.addFlashAttribute("resultStatus", "error");
            redirectAttributes.addFlashAttribute("resultMessage",
                    "Đã từ chối vé ưu đãi [" + rejected.getTicketCode() + "] (Ghế " + rejected.getSeatCode() + "). Ghế đã được nhả lại.");
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đã từ chối vé ưu đãi [" + rejected.getTicketCode() + "] (Lý do: " + reason + "). Nhắc khách liên hệ quầy mua vé thường.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("resultStatus", "error");
            redirectAttributes.addFlashAttribute("resultMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/checkin" + (tripId != null ? "?tripId=" + tripId : "");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Tham số tra cứu soát vé không hợp lệ.");
        return "redirect:/admin/checkin";
    }
}
