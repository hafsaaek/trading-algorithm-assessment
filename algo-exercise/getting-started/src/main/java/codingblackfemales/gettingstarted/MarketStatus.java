package codingblackfemales.gettingstarted;

import java.time.*;

public class MarketStatus {

    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(8, 0, 0);
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(16, 35, 0); // market close time is after close market auction window to allow our algo to secure a good ask/bid price
    private static final ZoneId LONDON_TIME_ZONE = ZoneId.of("Europe/London");

    public boolean isMarketClosed() {
        ZonedDateTime timeNow = ZonedDateTime.now(LONDON_TIME_ZONE); // Define London time zone & the present time
        LocalDate today = LocalDate.now(LONDON_TIME_ZONE); // Declare today's date according to London's time zone
        ZonedDateTime marketOpenDateTime = ZonedDateTime.of(today, MARKET_OPEN_TIME, LONDON_TIME_ZONE);  // Declare market opening conditions
        ZonedDateTime marketCloseDateTime = ZonedDateTime.of(today, MARKET_CLOSE_TIME, LONDON_TIME_ZONE); // Declare market closing conditions

        // Deduce if the current time is before opening, after closing, or on a weekend - we will ignore holidays for now
        return timeNow.isBefore(marketOpenDateTime) || timeNow.isAfter(marketCloseDateTime) || today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY; // Market is closed @ or after 4.35pm
    }
}