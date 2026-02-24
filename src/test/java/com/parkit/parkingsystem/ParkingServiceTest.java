package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

	private static final String REG_NUMBER = "ABCDEF";
	private static final int DEFAULT_SLOT_ID = 1;

	private ParkingService parkingService;

	@Mock
	private InputReaderUtil inputReaderUtil;
	@Mock
	private ParkingSpotDAO parkingSpotDAO;
	@Mock
	private TicketDAO ticketDAO;

	@BeforeEach
	void setUpPerTest() {
		parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
	}

	private Ticket buildTicket(Duration parkedSince, ParkingType type) {
		ParkingSpot parkingSpot = new ParkingSpot(DEFAULT_SLOT_ID, type, false);

		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - parkedSince.toMillis()));
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber(REG_NUMBER);
		return ticket;
	}

	private void mockIncoming(int selection, ParkingType type, int nextSlotId) throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(selection);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		when(parkingSpotDAO.getNextAvailableSlot(type)).thenReturn(nextSlotId);
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
	}

	@Test
	public void processExitingVehicleTest() throws Exception {
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		when(ticketDAO.getTicket(REG_NUMBER)).thenReturn(buildTicket(Duration.ofHours(1), ParkingType.CAR));
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(1);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

		parkingService.processExitingVehicle();

		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void processExitingVehicleTest_forCarOrBike(ParkingType type) throws Exception {
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		when(ticketDAO.getTicket(REG_NUMBER)).thenReturn(buildTicket(Duration.ofHours(1), type));
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(1);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

		parkingService.processExitingVehicle();

		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
	}

	@Test
	public void testProcessIncomingVehicle() throws Exception {
		mockIncoming(1, ParkingType.CAR, DEFAULT_SLOT_ID);
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(0);

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({ "1, CAR", "2, BIKE" })
	public void testProcessIncomingVehicle_forCarOrBike(int selection, ParkingType type) throws Exception {
		mockIncoming(selection, type, DEFAULT_SLOT_ID);
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(0);

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, times(1)).getNextAvailableSlot(type);
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
	}

	@Test
	public void testProcessIncomingVehicleInvalidSelection() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(3);

		parkingService.processIncomingVehicle();

		verify(inputReaderUtil, never()).readVehicleRegistrationNumber();
		verify(ticketDAO, never()).saveTicket(any());
		verify(parkingSpotDAO, never()).updateParking(any());
		verify(parkingSpotDAO, never()).getNextAvailableSlot(any());
	}

	@Test
	public void processExitingVehicleTestUnableUpdate() throws Exception {
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		when(ticketDAO.getTicket(REG_NUMBER)).thenReturn(buildTicket(Duration.ofHours(1), ParkingType.CAR));
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(0);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		parkingService.processExitingVehicle();

		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void processExitingVehicleTestUnableUpdate_forCarOrBike(ParkingType type) throws Exception {
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		when(ticketDAO.getTicket(REG_NUMBER)).thenReturn(buildTicket(Duration.ofHours(1), type));
		when(ticketDAO.getNbTicket(REG_NUMBER)).thenReturn(0);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		parkingService.processExitingVehicle();

		verify(ticketDAO, times(1)).getNbTicket(REG_NUMBER);
		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
	}

	@ParameterizedTest
	@CsvSource({ "1, CAR", "2, BIKE" })
	void testGetNextParkingNumberIfAvailable(int selection, ParkingType type) {
		when(inputReaderUtil.readSelection()).thenReturn(selection);
		when(parkingSpotDAO.getNextAvailableSlot(type)).thenReturn(DEFAULT_SLOT_ID);

		ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

		assertNotNull(result);
		assertEquals(DEFAULT_SLOT_ID, result.getId());
		assertTrue(result.isAvailable());
		assertEquals(type, result.getParkingType());
		verify(parkingSpotDAO, times(1)).getNextAvailableSlot(type);
	}

	@ParameterizedTest
	@CsvSource({ "1, CAR", "2, BIKE" })
	void testGetNextParkingNumberIfAvailableParkingNumberNotFound(int selection, ParkingType type) {
		when(inputReaderUtil.readSelection()).thenReturn(selection);
		when(parkingSpotDAO.getNextAvailableSlot(type)).thenReturn(-1);

		ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

		assertNull(result);
		verify(parkingSpotDAO, times(1)).getNextAvailableSlot(type);
	}

	@Test
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
		when(inputReaderUtil.readSelection()).thenReturn(3);

		ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

		assertNull(result);
		verify(parkingSpotDAO, never()).getNextAvailableSlot(any());
	}
}