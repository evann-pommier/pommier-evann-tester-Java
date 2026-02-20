package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

	private static final String REG_NUMBER = "ABCDEF";

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
		dataBasePrepareService.clearDataBaseEntries();
	}

	private void forceInTimeInDbForActiveTicket(String regNumber, Date newInTime) throws Exception {
		try (Connection con = dataBaseTestConfig.getConnection();
				PreparedStatement ps = con.prepareStatement(
						"UPDATE ticket SET in_time = ? WHERE vehicle_reg_number = ? AND out_time IS NULL")) {
			ps.setTimestamp(1, new java.sql.Timestamp(newInTime.getTime()));
			ps.setString(2, regNumber);
			ps.executeUpdate();
		}
	}

	@Test
	public void testParkingACar() throws Exception {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		parkingService.processIncomingVehicle();

		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(ticket, "Le ticket doit être créé en base");
		assertEquals(REG_NUMBER, ticket.getVehicleRegNumber(), "La plaque doit correspondre");
		assertNotNull(ticket.getInTime(), "Le inTime doit être renseigné");
		assertNull(ticket.getOutTime(), "Le outTime doit être null à l'entrée");

		int nextSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
		assertNotEquals(1, nextSlot, "La place 1 doit être occupée après l'entrée");
	}

	@Test
	public void testParkingLotExit() throws Exception {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();
		forceInTimeInDbForActiveTicket(REG_NUMBER, new Date(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h
		parkingService.processExitingVehicle();

		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(ticket, "Le ticket doit exister en base");
		assertNotNull(ticket.getOutTime(), "Le outTime doit être renseigné à la sortie");
		assertTrue(ticket.getPrice() > 0, "Le prix doit être calculé (durée > 30min)");

		int nextSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
		assertEquals(1, nextSlot, "La place 1 doit redevenir disponible après la sortie");
	}

	@Test
	public void testParkingLotExitRecurringUser() throws Exception {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		Ticket ticket = null;
		for (int i = 0; i < 2; i++) {
			parkingService.processIncomingVehicle();
			forceInTimeInDbForActiveTicket(REG_NUMBER, new Date(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h
			parkingService.processExitingVehicle();
			ticket = ticketDAO.getTicket(REG_NUMBER);
		}

		assertNotNull(ticket);
		assertNotNull(ticket.getOutTime());

		double durationHours = (ticket.getOutTime().getTime() - ticket.getInTime().getTime()) / (1000.0 * 60 * 60);
		double expected = durationHours * Fare.CAR_RATE_PER_HOUR * 0.95;

		assertEquals(expected, ticket.getPrice(), 0.01, "Le prix doit inclure la remise 5%");
	}
}
