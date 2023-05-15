package com.parkit.parkingsystem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Integration Test class that contains 3 methods that check the interactions
 * between the methods called during a classic use of the application. Deals
 * with incoming vehicle case, exiting vehicle case and exiting recurrent
 * vehicle case.
 *
 * @author Sébastien Cappon, [NPC]Tek
 * @version 2.0
 */
@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

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
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

	@AfterAll
	private static void tearDown() {

	}

	/**
	 * Tests the entry of a car into the parking lot.
	 *
	 * @result A first ticket is created. The parking slot is no longer available.
	 * @return void
	 */
	@Test
	public void testParkingACar() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		Ticket incomingVehicleTicket = ticketDAO.getTicket("ABCDEF");
		ParkingSpot incomingVehicleParkingSpot = incomingVehicleTicket.getParkingSpot();

		assertThat(incomingVehicleTicket.getId()).isEqualTo(1);
		assertThat(incomingVehicleParkingSpot.isAvailable()).isFalse();
	}

	/**
	 * Tests the exit of a car from the parking lot.
	 * 
	 * @note The execution time of the test is too short to provide sufficient time
	 *       for the calculation of the costs between calling method
	 *       <code>processIncomingVehicle</code> and method
	 *       <code>processExitingVehicle</code>. It is necessary to modify the
	 *       <code>inTime</code> value by reinjection it, minus an hour, into the
	 *       database.
	 * @result The out time is determined. The fees have been calculated.
	 * @return void
	 */
	@Test
	public void testParkingLotExit() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		Ticket incomingVehicleTicket = ticketDAO.getTicket("ABCDEF");
		Date theHour = new Date(60 * 60 * 1000);
		Date theHourBefore = new Date(incomingVehicleTicket.getInTime().getTime() - theHour.getTime());
		incomingVehicleTicket.setInTime(theHourBefore);

		Connection conn = null;
		try {
			conn = ticketDAO.dataBaseConfig.getConnection();
			PreparedStatement pstmt = conn.prepareStatement("UPDATE ticket SET IN_TIME = ? WHERE ID = ?");
			pstmt.setTimestamp(1, new Timestamp(incomingVehicleTicket.getInTime().getTime()));
			pstmt.setInt(2, incomingVehicleTicket.getId());
			pstmt.execute();
		} catch (Exception er) {
			er.printStackTrace();
			throw new RuntimeException("Failed to update test ticket with earlier inTime value");
		} finally {
			ticketDAO.dataBaseConfig.closeConnection(conn);
		}

		parkingService.processExitingVehicle();

		Ticket exitingVehicleTicket = ticketDAO.getTicket("ABCDEF");

		assertThat(exitingVehicleTicket.getOutTime()).isNotNull();
		assertThat(exitingVehicleTicket.getPrice()).isNotNull();
	}

	/**
	 * Tests the exit of a recurrent car from the parking lot.
	 * 
	 * @note The execution time of the test is too short to provide sufficient time
	 *       for the calculation of the costs between calling method
	 *       <code>processIncomingVehicle</code> and method
	 *       <code>processExitingVehicle</code>. It is necessary to modify the
	 *       <code>inTime</code> value by reinjection it, minus an hour, into the
	 *       database.
	 * @result The ticket price corresponds to 95% of the fees for one hour of
	 *         parking for a car.
	 * @return void
	 */
	@Test
	public void testParkingLotExitRecurringUser() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();
		parkingService.processExitingVehicle();
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