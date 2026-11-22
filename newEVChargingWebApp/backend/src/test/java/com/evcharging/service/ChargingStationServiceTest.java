package com.evcharging.service;

import com.evcharging.dto.StationRequest;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.Roles;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChargingStationServiceTest {

    private final ChargingStationRepository stations = mock(ChargingStationRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final ChargingStationService service = new ChargingStationService(stations, bookings);

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(int userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(userId, "u" + userId + "@test.com", role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private StationRequest request(int totalSlots) {
        return new StationRequest("City Charge", "MG Road, Pune", totalSlots, new BigDecimal("12.50"),
                "DC Fast", null, 18.52, 73.85);
    }

    private ChargingStation station(int total, int available, Integer ownerId) {
        ChargingStation s = new ChargingStation();
        s.setstationId(1);
        s.setstationName("Old");
        s.setsLocation("Old place");
        s.settotalSlots(total);
        s.setavailableSlots(available);
        s.setStatus("AVAILABLE");
        s.setOwnerId(ownerId);
        return s;
    }

    @Test
    void addStationStoresCoordinatesOwnerAndFullAvailability() {
        loginAs(7, Roles.STATION_OWNER);
        when(stations.save(any(ChargingStation.class))).thenAnswer(inv -> inv.getArgument(0));

        ChargingStation saved = service.addStation(request(6));

        assertEquals(6, saved.getavailableSlots());
        assertEquals(6, saved.gettotalSlots());
        assertEquals(7, saved.getOwnerId());
        assertEquals(18.52, saved.getLatitude());
        assertEquals(73.85, saved.getLongitude());
        assertEquals("AVAILABLE", saved.getStatus());
    }

    @Test
    void adminCanUpdateAnyStationAndKeepsSlotsInUse() {
        loginAs(1, Roles.ADMIN);
        ChargingStation existing = station(10, 4, 99); // 6 slots in use
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(existing));
        when(stations.save(any(ChargingStation.class))).thenAnswer(inv -> inv.getArgument(0));

        ChargingStation updated = service.updateStation(1, request(8));

        assertEquals(8, updated.gettotalSlots());
        assertEquals(2, updated.getavailableSlots()); // 8 total - 6 in use
        assertEquals("City Charge", updated.getstationName());
    }

    @Test
    void totalSlotsCannotGoBelowSlotsInUse() {
        loginAs(1, Roles.ADMIN);
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(station(10, 4, 1)));

        assertThrows(IllegalArgumentException.class, () -> service.updateStation(1, request(5)));
        verify(stations, never()).save(any());
    }

    @Test
    void ownerCannotEditSomeoneElsesStation() {
        loginAs(7, Roles.STATION_OWNER);
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(station(10, 10, 99)));

        assertThrows(AccessDeniedException.class, () -> service.updateStation(1, request(10)));
    }

    @Test
    void ownerCanEditOwnStation() {
        loginAs(7, Roles.STATION_OWNER);
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(station(10, 10, 7)));
        when(stations.save(any(ChargingStation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("City Charge", service.updateStation(1, request(10)).getstationName());
    }

    @Test
    void stationWithBookingHistoryCannotBeDeleted() {
        loginAs(1, Roles.ADMIN);
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(station(10, 10, 1)));
        when(bookings.existsByStationId(1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteStation(1));
        verify(stations, never()).delete(any());
    }

    @Test
    void stationWithoutBookingsIsDeleted() {
        loginAs(1, Roles.ADMIN);
        ChargingStation existing = station(10, 10, 1);
        when(stations.findByIdForUpdate(1)).thenReturn(Optional.of(existing));
        when(bookings.existsByStationId(1)).thenReturn(false);

        service.deleteStation(1);

        verify(stations).delete(existing);
    }
}
