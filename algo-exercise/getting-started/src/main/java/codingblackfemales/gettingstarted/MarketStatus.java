package codingblackfemales.gettingstarted;

/* Market Status Interface to mock in tests and avoid mocking concrete implementation */

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public interface MarketStatus {

    boolean isMarketOpen();

    default MARKET_PHASES getMarketPhase(ZonedDateTime currentTime) {
        return MARKET_PHASES.OPEN;
    };

    //


}
