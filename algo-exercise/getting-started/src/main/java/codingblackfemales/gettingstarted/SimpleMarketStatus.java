package codingblackfemales.gettingstarted;

import java.time.*;

public class SimpleMarketStatus implements MarketStatus {
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(8, 0, 0);
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(16, 35, 0); // market close time is after close market auction window to allow our algo to secure a good ask/bid price
    private static final ZoneId LONDON_TIME_ZONE = ZoneId.of("Europe/London");

    @Override
    public boolean isMarketOpen() {
        ZonedDateTime timeNow = ZonedDateTime.now(LONDON_TIME_ZONE); // Define London time zone & the present time
        LocalDate today = timeNow.toLocalDate(); // Declare today's date according to London's time zone
        ZonedDateTime marketOpenDateTime = ZonedDateTime.of(today, MARKET_OPEN_TIME, LONDON_TIME_ZONE);  // Declare market opening conditions
        ZonedDateTime marketCloseDateTime = ZonedDateTime.of(today, MARKET_CLOSE_TIME, LONDON_TIME_ZONE); // Declare market closing conditions

        // Deduce if the current time is before opening, after closing, or on a weekend - we will ignore holidays for now
        return today.getDayOfWeek() != DayOfWeek.SATURDAY && today.getDayOfWeek() != DayOfWeek.SUNDAY && timeNow.isAfter(marketOpenDateTime) && timeNow.isBefore(marketCloseDateTime); // Market is open at 8am and closed before 8am & after 4.35pm & on weekend
    }
/*** in exit method:
     * MarketStatus marketStatus = new SimpleMarketStatus();
     * StretchAlgoLogic stretchAlgoLogic = new StretchAlgoLogic(marketStatus, new OrderBookService(), new MovingWeightAverageCalculator())
     * stretchAlgoLogic.evaluate() // run the algo
*/
}
