package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}
	public void calculateFare(Ticket ticket, boolean discount) {
	    if (ticket.getOutTime() == null || ticket.getOutTime().before(ticket.getInTime())) {
	        throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
	    }

	    long durationMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
	    if (durationMillis < 30L * 60 * 1000) {
	        ticket.setPrice(0.0);
	        return;
	    }
	    double durationHours = durationMillis / (1000.0 * 60 * 60); // durée en heures (double)

	    switch (ticket.getParkingSpot().getParkingType()) {
	        case CAR:
	        	if(discount) {ticket.setPrice((durationHours * Fare.CAR_RATE_PER_HOUR)*0.95);}
	        	else {ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);}
	            break;
	        case BIKE:
	        	if(discount) {ticket.setPrice((durationHours * Fare.BIKE_RATE_PER_HOUR)*0.95);}
	        	else {ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);}
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown Parking Type");
	    }
	}
}