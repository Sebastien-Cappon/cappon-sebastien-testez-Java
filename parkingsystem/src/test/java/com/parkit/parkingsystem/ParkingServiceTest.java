package com.parkit.parkingsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

/**
 * Contains 6 tests of the vehicle entry and exit methods, as well as the
 * parking space allocation method.
 *
 * @author Sébastien Cappon, [NPC]Tek
 * @version 2.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingServiceTest {

	private static ParkingService parkingService;

	@Mock
	private static InputReaderUtil inputReaderUtil;
	@Mock
	private static ParkingSpotDAO parkingSpotDAO;
	@Mock
	private static TicketDAO ticketDAO;

	@BeforeEach
	private void setUpPerTest() {
		try {
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
			when(ticketDAO.getNbTicket(any(String.class))).thenReturn(2);

			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

			parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to set up test mock objects");
		}
	}

	/**
	 * Tests the entry of a car in the parking lot.
	 *
	 * @result The parking lot should be updated. A ticket must be saved. The number of current and previous tickets for the incoming car must be counted.
	 * @return void
	 */
    @Test
	public void testProcessIncomingVehicle() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
		
		parkingService.processIncomingVehicle();
		
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
	}

	/**
	 * Tests the exit of a vehicle from the parking lot.
	 *
	 * @result The parking lot should be updated. The ticket must be collected and
	 *         updated. The number of current and previous tickets for the incoming
	 *         car must be counted.
	 * @return void
	 */
	@Test
	public void processExitingVehicleTest() {
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	/**
	 * Tests the failure of the ticket update when a car leaves the parking lot.
	 *
	 * @result The ticket must be collected and updated. The number of current and previous tickets for the incoming car must be counted.
	 * @return void
	 */
    @Test
	public void processExitingVehicleTestUnableUpdate() {
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
		
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
	}

	/**
	 * Tests the retrieving of the next empty car parking spot.
	 *
	 * @result The next empty car parking slot must be searched. The Id of the parking slot given to the first car entering the parking lot is 1. This parking slot is available.
	 * @return void
	 */
    @Test
	public void testGetNextParkingNumberIfAvailable() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
		
		ParkingSpot nextParkingSlot = parkingService.getNextParkingNumberIfAvailable();

		verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(ParkingType.CAR);
		assertThat(nextParkingSlot.getId()).isEqualTo(1);
		assertThat(nextParkingSlot.isAvailable()).isTrue();
	}

	/**
	 * Tests the failure of the search for the next empty car parking spot.
	 *
	 * @result The next empty car parking slot must be searched. No parking slot is found (null).
	 * @return void
	 */
    @Test
	public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);
		
		ParkingSpot noMoreParkingSlot = parkingService.getNextParkingNumberIfAvailable();

		verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(ParkingType.CAR);
		assertThat(noMoreParkingSlot).isNull();
	}

	/**
	 * Tests the failure of the search for the next empty parking spot for an unknown vehicle type, such as a spacecraft, for example.
	 *
	 * @result The next empty parking slot must be searched. No parking slot is found (null).
	 * @return void
	 */
    @Test
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
		when(inputReaderUtil.readSelection()).thenReturn(3);
		
		ParkingSpot noParkingSlotIfWrongInput = parkingService.getNextParkingNumberIfAvailable();

		assertThat(noParkingSlotIfWrongInput).isNull();  
	}
}