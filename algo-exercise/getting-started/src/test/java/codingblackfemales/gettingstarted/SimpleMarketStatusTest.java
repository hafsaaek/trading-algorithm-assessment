package codingblackfemales.gettingstarted;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

public class SimpleMarketStatusTest {

    private SimpleMarketStatus marketStatus = new SimpleMarketStatus();
    @Test
    public void testIsMarketOpenMethod() {
        // Check the function returns true when the market is open in real time and false when it's closed in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 0);

        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(marketStatus.isMarketOpen());

        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertEquals(isMarketClosedTestVariable, !marketStatus.isMarketOpen());
    }

    // 6 test cases: one a sunday, saturday, before 8am, after 4.35pm
    // stretch market status logic: is the market open at this time (current  version) instead of is the market open now?

}
