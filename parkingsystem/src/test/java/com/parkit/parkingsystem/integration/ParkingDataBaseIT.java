package com.parkit.parkingsystem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket incomingVehicleTicket = ticketDAO.getTicket("ABCDEF");
        ParkingSpot incomingVehicleParkingSpot = incomingVehicleTicket.getParkingSpot();

        assertEquals(incomingVehicleTicket.getId(), 1);
        assertFalse(incomingVehicleParkingSpot.isAvailable());
    }

    @Test
	public void testParkingLotExit() {
		testParkingACar();
		
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processExitingVehicle();

		Ticket exitingVehicleTicket = ticketDAO.getTicket("ABCDEF");

		assertThat(exitingVehicleTicket.getPrice()).isNotNull();
		assertThat(exitingVehicleTicket.getOutTime()).isNotNull();
	}

    @Test
    public void testParkingLotExitRecurringUser() {
        testParkingLotExit();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

        Ticket incomingRecurringVehicleTicket = ticketDAO.getTicket("ABCDEF");
        Date oneHour = new Date(60 * 60 * 1000);
        Date oneHourBefore = new Date(incomingRecurringVehicleTicket.getInTime().getTime() - oneHour.getTime());
        incomingRecurringVehicleTicket.setInTime(oneHourBefore);
        
        Connection con = null;
		try {
			con = ticketDAO.dataBaseConfig.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE ticket SET IN_TIME = ? WHERE ID = ?");
			ps.setTimestamp(1, new Timestamp(incomingRecurringVehicleTicket.getInTime().getTime()));
			ps.setInt(2, incomingRecurringVehicleTicket.getId());
			ps.execute();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to update test ticket with earlier inTime value");
		} finally {
			ticketDAO.dataBaseConfig.closeConnection(con);
		}
        
        parkingService.processExitingVehicle();
        
        Ticket incomingRecurringVehicleTicketUpToDate = ticketDAO.getTicket("ABCDEF");

        assertThat(Math.round(incomingRecurringVehicleTicketUpToDate.getPrice() * 1000.0) / 1000.0)
            .isEqualTo(Math.round(0.95 * Fare.CAR_RATE_PER_HOUR * 1000.0) / 1000.0);
    }
}