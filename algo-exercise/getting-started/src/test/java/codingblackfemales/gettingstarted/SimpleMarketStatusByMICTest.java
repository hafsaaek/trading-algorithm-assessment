package codingblackfemales.gettingstarted;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.Assert.*;

public class SimpleMarketStatusByMICTest {

    private  SimpleMarketStatusByMIC micSimpleMarketStatus = new SimpleMarketStatusByMIC();
    LocalDate today = LocalDate.now();

//    @Test
//    public void testIsMarketOpenXLON() {
//        ZonedDateTime currentTime = ZonedDateTime.of(today, LocalTime.now(), ZoneId.of("Europe/London"));
//        assertEquals(MarketStatusByMIC.MARKET_PHASES.OPEN, micSimpleMarketStatus.getMarketPhase(currentTime, "XLON"));
//
//    }

    @Test
    public void testOpenAuctionTimeWindow() {
        ZonedDateTime currentTime = ZonedDateTime.of(today, LocalTime.of(7, 59, 0), ZoneId.of("Europe/London"));
        assertEquals(MARKET_PHASES.OPEN_AUCTION, micSimpleMarketStatus.getMarketPhase(currentTime, "XLON"));
    }

    @Test
    public void testOpenTimeWindow() {
        ZonedDateTime currentTime = ZonedDateTime.of(today, LocalTime.of(8, 59, 0), ZoneId.of("Europe/London"));
        assertEquals(MARKET_PHASES.OPEN, micSimpleMarketStatus.getMarketPhase(currentTime, "XLON"));
    }

    @Test
    public void testClosedAuctionTimeWindowLondon() {
        ZonedDateTime currentTime = ZonedDateTime.of(today, LocalTime.of(16, 34, 58), ZoneId.of("Europe/London"));
        assertEquals(MARKET_PHASES.CLOSING_AUCTION, micSimpleMarketStatus.getMarketPhase(currentTime, "XLON"));
    }

    @Test
    public void testIsNYSEOpenOnATuesdayEvening() {
        ZonedDateTime currentTime = ZonedDateTime.of(today, LocalTime.of(19, 30, 0), ZoneId.of("America/New_York"));
        assertEquals(MARKET_PHASES.CLOSED, micSimpleMarketStatus.getMarketPhase(currentTime, "XNYS"));
    }

    @Test
    public void testHongKongMarketClosedSaturday() {
        ZonedDateTime currentTime = ZonedDateTime.of(LocalDate.of(2024, 10, 26), LocalTime.of(12, 59, 0), ZoneId.of("Asia/Hong_Kong"));
        assertEquals(MARKET_PHASES.CLOSED, micSimpleMarketStatus.getMarketPhase(currentTime, "XHKG"));
    }

    @Test
    public void testAustraliaMarketClosedSunday() {
        ZonedDateTime currentTime = ZonedDateTime.of(LocalDate.of(2024, 10, 27), LocalTime.of(8, 59, 0), ZoneId.of("Australia/Sydney"));
        assertEquals(MARKET_PHASES.CLOSED, micSimpleMarketStatus.getMarketPhase(currentTime, "XASX"));
    }
}
