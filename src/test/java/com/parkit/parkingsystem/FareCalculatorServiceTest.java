package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;

public class FareCalculatorServiceTest {

	private static FareCalculatorService fareCalculatorService;
	private Ticket ticket;

	@BeforeAll
	private static void setUp() {
		fareCalculatorService = new FareCalculatorService();
	}

	@BeforeEach
	private void setUpPerTest() {
		ticket = new Ticket();
	}

	private void initTicket(ParkingType type, long durationMs) {
		Date out = new Date();
		Date in = new Date(out.getTime() - durationMs);
		ticket.setInTime(in);
		ticket.setOutTime(out);
		ticket.setParkingSpot(new ParkingSpot(1, type, false));
	}

	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicle(ParkingType type) {
		initTicket(type, 60 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket);
		switch (type) {
			case CAR:
				assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.01);
				break;
			case BIKE:
				assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.01);
		}
	}

	@Test
	public void calculateFareUnknownType() {
		initTicket(null, 60 * 60 * 1000);
		assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
	}

	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicleWithFutureInTime(ParkingType type) {
		Date outTime = new Date();
		Date inTime = new Date(outTime.getTime() + (60 * 60 * 1000));
		ParkingSpot parkingSpot = new ParkingSpot(1, type, false);
		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);

		assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
	}

	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicleWithLessThanOneHourParkingTime(ParkingType type) {
		initTicket(type, 45 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket);
		switch (type) {
			case BIKE:
				assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice(), 0.01);
				break;
			case CAR:
				assertEquals((0.75 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice(), 0.01);
				break;
		}
	}

	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicleWithMoreThanADayParkingTime(ParkingType type) {
		initTicket(type, 24 * 60 * 60 * 1000);

		fareCalculatorService.calculateFare(ticket);

		switch (type) {
			case BIKE:
				assertEquals((24 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice(), 0.01);
				break;
			case CAR:
				assertEquals((24 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice(), 0.01);
				break;
		}
	}

	@Test
	public void calculateFareCarWithLessThan30minutesParkingTime() {
		initTicket(ParkingType.CAR, 29 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket);
		assertEquals(0.0, ticket.getPrice(), 0.01);
	}

	@Test
	public void calculateFareBikeWithLessThan30minutesParkingTime() {
		initTicket(ParkingType.BIKE, 29 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket);
		assertEquals(0.0, ticket.getPrice(), 0.01);
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicleWithLessThan30minutesParkingTime(ParkingType type) {
		initTicket(type, 29 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket);
		assertEquals((0.0), ticket.getPrice());
	}

	@Test
	public void calculateFareCarWithDiscount() {
		initTicket(ParkingType.CAR, 60 * 60 * 1000); // > 30min
		fareCalculatorService.calculateFare(ticket, true);
		assertEquals(Fare.CAR_RATE_PER_HOUR * 0.95, ticket.getPrice(), 0.01);
	}

	@Test
	public void calculateFareBikeWithDiscount() {
		initTicket(ParkingType.BIKE, 60 * 60 * 1000); // > 30min
		fareCalculatorService.calculateFare(ticket, true);
		assertEquals(Fare.BIKE_RATE_PER_HOUR * 0.95, ticket.getPrice(), 0.01);
	}

	@Disabled
	@ParameterizedTest
	@CsvSource({ "CAR", "BIKE" })
	public void calculateFareVehicleWithDiscount(ParkingType type) {
		initTicket(type, 60 * 60 * 1000);
		fareCalculatorService.calculateFare(ticket, true);
		switch (type) {
			case BIKE:
				assertEquals(Fare.BIKE_RATE_PER_HOUR * 1 * 0.95, ticket.getPrice(), 0.01);
				break;
			case CAR:
				assertEquals(Fare.CAR_RATE_PER_HOUR * 1 * 0.95, ticket.getPrice(), 0.01);
				break;
		}
	}
}