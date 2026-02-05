package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {
	
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    void setUpPerTest() {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }
    
    private Ticket buildDefaultTicket() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        return ticket;
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(buildDefaultTicket());
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(0);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }
    
    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(buildDefaultTicket());
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(0);

        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertTrue(result.isAvailable());
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        when(inputReaderUtil.readSelection()).thenReturn(3); // invalide

        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        assertNull(result);
        verify(parkingSpotDAO, never()).getNextAvailableSlot(any());
    }
}
