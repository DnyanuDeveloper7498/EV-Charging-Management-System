package com.evcharging.controller;

import com.evcharging.dto.CancelBookingRequest;
import com.evcharging.dto.CreateBookingRequest;
import com.evcharging.dto.RescheduleBookingRequest;
import com.evcharging.model.Booking;
import com.evcharging.model.BookingStatus;
import com.evcharging.service.BookingService;
import com.evcharging.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final NotificationService notificationService;

    public BookingController(BookingService bookingService, NotificationService notificationService) {
        this.bookingService = bookingService;
        this.notificationService = notificationService;
    }

    //---------- ADMIN only-----------
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{bookingId}")
    public Booking getBooking(@PathVariable int bookingId) {
        return bookingService.findByBookingId(bookingId);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<Booking> getBookingsByVehicle(@PathVariable int vehicleId) {
        return bookingService.findByVehicleId(vehicleId);
    }

    //---------- ADMIN only-----------
    @GetMapping("/station/{stationId}")
    public List<Booking> getBookingsByStation(@PathVariable int stationId) {
        return bookingService.findByStationId(stationId);
    }

    //---------- ADMIN only-----------
    @GetMapping("/status/{bookingStatus}")
    public List<Booking> getBookingsByStatus(@PathVariable String bookingStatus) {
        return bookingService.findByBookingStatus(bookingStatus);
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getBookingsByUser(@PathVariable int userId) {
        return bookingService.findByUserId(userId);
    }


   //-------------- create booking --------------------

    @PostMapping
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody CreateBookingRequest request) {

        Booking booking = bookingService.createBooking(request);

        if (BookingStatus.CONFIRMED.equals(booking.getbookingStatus())) {
            notificationService.bookingConfirmed(booking);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    // -------------------------- reschedule -------------------------------

    @PutMapping("/{bookingId}/reschedule")
    public ResponseEntity<Booking> rescheduleBooking(@PathVariable int bookingId, @Valid @RequestBody RescheduleBookingRequest request) {

        Booking updatedBooking = bookingService.rescheduleBooking(bookingId, request);

        return ResponseEntity.ok(updatedBooking);
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, String>> cancelBooking(@PathVariable int bookingId, @Valid @RequestBody(required = false) CancelBookingRequest request) {
        bookingService.cancelBooking(bookingId, request);
        return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully."));
    }


    @PutMapping("/{bookingId}/approve")
    public ResponseEntity<Booking> approveBooking(@PathVariable int bookingId) {

        Booking booking = bookingService.approveBooking(bookingId);

        return ResponseEntity.ok(booking);
    }


    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<Booking> rejectBooking(@PathVariable int bookingId) {

        Booking booking = bookingService.rejectBooking(bookingId);

        return ResponseEntity.ok(booking);
    }


    // -------------------------- owner -------------------------------

    @GetMapping("/owner/pending")
    public ResponseEntity<List<Booking>> getPendingBookingsForOwner() {

        List<Booking> bookings = bookingService.getPendingBookingsForOwner();

        return ResponseEntity.ok(bookings);
    }
}
