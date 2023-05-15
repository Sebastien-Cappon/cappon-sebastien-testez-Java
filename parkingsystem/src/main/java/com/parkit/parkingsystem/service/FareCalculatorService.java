package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

/**
 * Contains 2 methods for the calculation of the parking fees.
 *
 * @author [NPC]Tek, Sébastien Cappon
 * @version 2.0
 */
public class FareCalculatorService {

	/**
	 * Calculates the parking fees for a given vehicle, recurrent or not.
	 *
	 * @param ticket   Ticket parameter that contains the data needed to calculate
	 *                 the parking fees.
	 * @param discount Boolean parameter which determines whether the calculation of
	 *                 the parking fees should take into the 5% discount or not.
	 * @return void
	 */
	public void calculateFare(Ticket ticket, boolean discount) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		long inHour = ticket.getInTime().getTime();
		long outHour = ticket.getOutTime().getTime();
		double duration = (outHour - inHour) / (60.0 * 60.0 * 1000.0);

		if (duration > 0.5) {
			switch (ticket.getParkingSpot().getParkingType()) {
			case CAR: {
				ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
				break;
			}
			case BIKE: {
				ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
				break;
			}
			default:
				throw new IllegalArgumentException("Unkown Parking Type");
			}

			if (discount) {
				ticket.setPrice(ticket.getPrice() * 0.95);
			}
		} else {
			ticket.setPrice(0);
		}
	}

	/**
	 * Calls the <code>calculateFare(Ticket ticket, boolean discount)</code> method
	 * by setting <code>discount</code> to <code>false</code>.
	 *
	 * @param ticket Full ticket generated when a vehicle enters the parking lot and
	 *               updated when it leaves.
	 * @return void
	 */
	public void calculateFare(Ticket ticket) {
		FareCalculatorService fareCalculatorService = new FareCalculatorService();
		fareCalculatorService.calculateFare(ticket, false);
	}
}