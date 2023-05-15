package com.parkit.parkingsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;

/**
 * Contains 11 tests of the parking fee calculation methods.
 *
 * @author [NPC]Tek, Sébastien Cappon
 * @version 2.0
 */
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

	/**
	 * Tests the calculation of the fee for one hour of parking for a car.
	 *
	 * @result The price of the ticket corresponds to the fees for one hour of
	 *         parking for a car.
	 * @return void
	 */
	@Test
	public void calculateFareCar() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(Fare.CAR_RATE_PER_HOUR);
	}

	/**
	 * Tests the calculation of the fee for one hour of parking for a bike.
	 *
	 * @result The price of the ticket corresponds to the fees for one hour of
	 *         parking for a bike.
	 * @return void
	 */
	@Test
	public void calculateFareBike() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(Fare.BIKE_RATE_PER_HOUR);
	}

	/**
	 * Tests the exception thrown when a vehicle of unknown type is registered.
	 *
	 * @result A <code>NullPointerException</code> is generated if the vehicle
	 *         specified is null.
	 * @return void
	 */
	@Test
	public void calculateFareUnkownType() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, null, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);

		assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
	}

	/**
	 * Tests the exception thrown when the entry time is later than the exit time
	 * for a bike.
	 *
	 * @result An <code>IllegalArgumentException</code> is generated if inTime value
	 *         is greater than outTime value.
	 * @return void
	 */
	@Test
	public void calculateFareBikeWithFutureInTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() + 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);

		assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
	}

	/**
	 * Tests the calculation of the fee for 45 minutes of parking for a bike. The
	 * charges are calculated on a prorata temporis basis of one hour of parking.
	 *
	 * @result The ticket price of the ticket corresponds to 75% of the fees for one
	 *         hour of parking for a bike.
	 * @return void
	 */
	@Test
	public void calculateFareBikeWithLessThanOneHourParkingTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 45 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(0.75 * Fare.BIKE_RATE_PER_HOUR);
	}

	/**
	 * Tests the calculation of the fee for 45 minutes of parking for a car. The
	 * fees are calculated on a prorata temporis basis of one hour of parking.
	 *
	 * @result The ticket price of the ticket corresponds to 75% of the fees for one
	 *         hour of parking for a car.
	 * @return void
	 */
	@Test
	public void calculateFareCarWithLessThanOneHourParkingTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 45 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(0.75 * Fare.CAR_RATE_PER_HOUR);
	}

	/**
	 * Tests the calculation of the fee for a day of parking for a car.
	 * 
	 * @result The ticket price of the ticket corresponds to 24 times the fees for
	 *         one hour of parking for a car.
	 * @return void
	 */
	@Test
	public void calculateFareCarWithMoreThanADayParkingTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 24 * 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(24 * Fare.CAR_RATE_PER_HOUR);
	}

	/**
	 * Tests the calculation of the fee for 20 minutes of parking for a car. Parking
	 * is free for less than 30 minutes.
	 *
	 * @result The price of the ticket is 0.
	 * @return void
	 */
	@Test
	public void calculateFareCarWithLessThan30minutesParkingTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 20 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(0);
	}

	/**
	 * Tests the calculation of the fee for 20 minutes of parking for a bike.
	 * Parking is free for less than 30 minutes.
	 *
	 * @result The price of the ticket is 0.
	 * @return void
	 */
	@Test
	public void calculateFareBikeWithLessThan30minutesParkingTime() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 20 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket);

		assertThat(ticket.getPrice()).isEqualTo(0);
	}

	/**
	 * Tests the calculation of the fee for an hour of parking for a recurring car.
	 * Recurring cars receive a 5% discount.
	 *
	 * @result The ticket price corresponds to 95% of the fees for one hour of
	 *         parking for a car.
	 * @return void
	 */
	@Test
	public void calculateFareCarWithDiscount() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket, true);

		assertThat(ticket.getPrice()).isEqualTo(0.95 * Fare.CAR_RATE_PER_HOUR);
	}

	/**
	 * Tests the calculation of the fee for an hour of parking for a recurring bike.
	 * Recurring bikes receive a 5% discount.
	 *
	 * @result The ticket price corresponds to 95% of the fees for one hour of
	 *         parking for a bike.
	 * @return void
	 */
	@Test
	public void calculateFareBikeWithDiscount() {
		Date inTime = new Date();
		Date outTime = new Date();
		inTime.setTime(outTime.getTime() - 60 * 60 * 1000);
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

		ticket.setInTime(inTime);
		ticket.setOutTime(outTime);
		ticket.setParkingSpot(parkingSpot);
		fareCalculatorService.calculateFare(ticket, true);

		assertThat(ticket.getPrice()).isEqualTo(0.95 * Fare.BIKE_RATE_PER_HOUR);
	}
}