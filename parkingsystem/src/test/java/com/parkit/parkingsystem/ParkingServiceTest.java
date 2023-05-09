package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(ticketDAO.getNbTicket(any(String.class))).thenReturn(2);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
	public void testProcessIncomingVehicle() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
		
		parkingService.processIncomingVehicle();
		
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
	}

    @Test
    public void processExitingVehicleTest(){
        parkingService.processExitingVehicle();
        
		verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
	public void processExitingVehicleTestUnableUpdate() {
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
		
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
	}

    @Test
	public void testGetNextParkingNumberIfAvailable() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
		
		ParkingSpot nextParkingSpot = parkingService.getNextParkingNumberIfAvailable();

		verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(ParkingType.CAR);
		assertEquals(nextParkingSpot.getId(), 1);
		assertTrue(nextParkingSpot.isAvailable());
	}

    @Test
	public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);
		
		ParkingSpot noMoreParkingSpot = parkingService.getNextParkingNumberIfAvailable();

		verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(ParkingType.CAR);
		assertEquals(noMoreParkingSpot, null);
	}

    @Test
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
		when(inputReaderUtil.readSelection()).thenReturn(3);
		
		ParkingSpot noParkingSpotIfWrongInput = parkingService.getNextParkingNumberIfAvailable();

		assertEquals(noParkingSpotIfWrongInput, null);   
	}
}
