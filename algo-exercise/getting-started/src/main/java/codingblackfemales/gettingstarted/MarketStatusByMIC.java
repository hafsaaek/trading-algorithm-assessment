package codingblackfemales.gettingstarted;

import java.time.ZonedDateTime;

public interface MarketStatusByMIC {

    enum MARKET_PHASES {
        OPEN_AUCTION,
        OPEN,
        CLOSING_AUCTION,
        CLOSED
    }

    MARKET_PHASES getMarketPhase(ZonedDateTime currentTime, String marketIdentifierCode);


}
