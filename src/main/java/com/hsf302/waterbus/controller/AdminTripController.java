package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.dto.TripCancellationPreviewDTO;
import com.hsf302.waterbus.dto.WaterbusDTO;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.TripService;
import com.hsf302.waterbus.service.WaterbusService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Controller
@RequestMapping("/admin/trips")
public class AdminTripController {
    private static final DateTimeFormatter DATE_TIME_LOCAL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final TripService tripService;
    private final RouteService routeService;
    private final WaterbusService waterbusService;

    public AdminTripController(TripService tripService, RouteService routeService, WaterbusService waterbusService) {
        this.tripService = tripService;
        this.routeService = routeService;
        this.waterbusService = waterbusService;
    }

    @InitBinder("trip")
    void bindTrip(WebDataBinder binder) {
        binder.setAllowedFields("routeId", "waterbusId", "departureTime", "status");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("trips", tripService.getTripsForAdmin());
        return "admin/trip/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("trip", new AdminTripForm());
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("trip") AdminTripForm trip, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                tripService.createAdminTrip(trip);
                flash.addFlashAttribute("successMessage", "Đã tạo chuyến tàu.");
                return "redirect:/admin/trips";
            } catch (IllegalArgumentException ex) {
                errors.reject("trip.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("trip.conflict", "Không thể tạo chuyến. Mã chuyến có thể bị trùng hoặc dữ liệu vừa thay đổi.");
            }
        }
        model.addAttribute("errorMessage", errors.getAllErrors().stream()
                .map(error -> error.getDefaultMessage()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" ")));
        return form(model);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable int id, @RequestParam(required = false) String reason,
                         RedirectAttributes flash) {
        try {
            tripService.cancelTrip(id, reason);
            flash.addFlashAttribute("successMessage", "Đã huỷ chuyến tàu.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/trips";
    }

    @GetMapping("/cancel")
    public String cancellationPreview(@RequestParam(required = false) Integer waterbusId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime departureFrom,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime departureTo,
                                      Model model) {
        return cancellationPage(model, waterbusId, departureFrom, departureTo, List.of());
    }

    @PostMapping("/cancel-selected")
    public String cancelSelected(@RequestParam(required = false) List<Integer> tripIds,
                                 @RequestParam(required = false) Integer waterbusId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime departureFrom,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime departureTo,
                                 @RequestParam(required = false) String reason,
                                 Model model, RedirectAttributes flash) {
        try {
            if (waterbusId == null || departureFrom == null || departureTo == null) {
                throw new IllegalArgumentException("Vui lòng lọc lại chuyến trước khi xác nhận hủy.");
            }
            tripService.cancelTrips(tripIds, reason, waterbusId, departureFrom, departureTo);
            flash.addFlashAttribute("successMessage",
                    "Đã huỷ " + tripIds.size()
                            + " chuyến. Các khoản hoàn đang chờ xử lý. Hãy chuyển tiền ngoài hệ thống rồi xác nhận tại đây.");
            return "redirect:/admin/refunds?status=PENDING";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return cancellationPage(model, waterbusId, departureFrom, departureTo, tripIds);
        }
    }

    private String cancellationPage(Model model, Integer waterbusId, LocalDateTime departureFrom,
                                    LocalDateTime departureTo, List<Integer> selectedTripIds) {
        List<WaterbusDTO> waterbuses = waterbusService.getWaterbusViews();
        model.addAttribute("waterbuses", waterbuses);
        model.addAttribute("waterbusId", waterbusId);
        model.addAttribute("departureFrom", departureFrom == null ? "" : departureFrom.format(DATE_TIME_LOCAL_FORMAT));
        model.addAttribute("departureTo", departureTo == null ? "" : departureTo.format(DATE_TIME_LOCAL_FORMAT));
        model.addAttribute("selectedTripIds", selectedTripIds == null ? List.of() : selectedTripIds);

        boolean hasFilter = waterbusId != null || departureFrom != null || departureTo != null;
        if (hasFilter) {
            if (waterbusId == null || departureFrom == null || departureTo == null) {
                model.addAttribute("errorMessage", "Vui lòng chọn tàu và đầy đủ thời gian bắt đầu, kết thúc.");
            } else if (departureFrom.isAfter(departureTo)) {
                model.addAttribute("errorMessage", "Thời gian bắt đầu phải trước hoặc bằng thời gian kết thúc.");
            } else if (waterbuses.stream().noneMatch(waterbus -> waterbus.id() != null
                    && waterbus.id().intValue() == waterbusId.intValue())) {
                model.addAttribute("errorMessage", "Không tìm thấy tàu đã chọn.");
            } else {
                List<TripCancellationPreviewDTO> previews =
                        tripService.getCancellableTrips(waterbusId, departureFrom, departureTo);
                model.addAttribute("cancellationPreviews", previews);
                model.addAttribute("filterApplied", true);
            }
        } else {
            model.addAttribute("cancellationPreviews", List.of());
            model.addAttribute("filterApplied", false);
        }
        return "admin/trip/cancel";
    }

    private String form(Model model) {
        model.addAttribute("minDepartureTime", LocalDateTime.now().withSecond(0).withNano(0)
                .plusMinutes(1).format(DATE_TIME_LOCAL_FORMAT));
        model.addAttribute("routes", routeService.getActiveRouteViews());
        model.addAttribute("waterbuses", waterbusService.getWaterbusViews().stream()
                .filter(waterbus -> "ACTIVE".equals(waterbus.status())).toList());
        return "admin/trip/form";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleInvalidParameter(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Mã chuyến hoặc tham số không hợp lệ.");
        return "redirect:/admin/trips";
    }
}
