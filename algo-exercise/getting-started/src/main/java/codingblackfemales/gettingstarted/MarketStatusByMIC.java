package codingblackfemales.gettingstarted;

import java.time.ZonedDateTime;

public interface MarketStatusByMIC {

    MARKET_PHASES getMarketPhase(ZonedDateTime currentTime, String marketIdentifierCode);


}
