package codingblackfemales.gettingstarted;

import java.time.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SimpleMarketStatusByMIC implements MarketStatusByMIC {

    private final HashMap<String, List<ZonedDateTime>> MIC = new HashMap<>();


    @Override
    public MARKET_PHASES getMarketPhase(ZonedDateTime currentTime, String marketIdentifierCode) {
        ZonedDateTime MARKET_OPEN_TIME = getMICMap().get(marketIdentifierCode).get(0);
        ZonedDateTime MARKET_CLOSE_TIME = getMICMap().get(marketIdentifierCode).get(1);
        ZonedDateTime MARKET_OPEN_AUCTION_TIME = getMICMap().get(marketIdentifierCode).stream().count() > 2? getMICMap().get(marketIdentifierCode).get(2) : null;
        ZonedDateTime MARKET_CLOSE_AUCTION_TIME = getMICMap().get(marketIdentifierCode).stream().count() > 2? getMICMap().get(marketIdentifierCode).get(3) : null;;

        DayOfWeek dayOfWeek = currentTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return MARKET_PHASES.CLOSED;
        }

        if( MARKET_OPEN_AUCTION_TIME != null && currentTime.isAfter(MARKET_OPEN_AUCTION_TIME) && currentTime.isBefore(MARKET_OPEN_TIME)){
            return MARKET_PHASES.OPEN_AUCTION;
        } else if (MARKET_CLOSE_AUCTION_TIME != null && currentTime.isAfter(MARKET_OPEN_TIME) && currentTime.isBefore(MARKET_CLOSE_AUCTION_TIME)) {
            return MARKET_PHASES.OPEN;
        } else if(MARKET_CLOSE_AUCTION_TIME != null && currentTime.isAfter(MARKET_CLOSE_AUCTION_TIME) && currentTime.isBefore(MARKET_CLOSE_TIME)){
            return MARKET_PHASES.CLOSING_AUCTION;
        } else {
            return MARKET_PHASES.CLOSED;
        }
    }

    public HashMap<String, List<ZonedDateTime>> getMICMap() {

        LocalDate today = LocalDate.now();

        /* London Stock Exchange opening and closing times: */
        ZonedDateTime XLON_OPEN_TIME = ZonedDateTime.of(today, LocalTime.of(8,0, 0), ZoneId.of("Europe/London"));
        ZonedDateTime XLON_CLOSE_TIME = ZonedDateTime.of(today, LocalTime.of(16, 35, 0),ZoneId.of("Europe/London"));
        ZonedDateTime XLON_OPEN_AUCTION_TIME = ZonedDateTime.of(today, LocalTime.of(7,50, 0), ZoneId.of("Europe/London"));
        ZonedDateTime XLON_CLOSE_AUCTION_TIME = ZonedDateTime.of(today, LocalTime.of(16,30, 0), ZoneId.of("Europe/London"));

        /* New York Stock Exchange NYSE opening and closing times: */
        ZonedDateTime XNYS_OPEN_TIME = ZonedDateTime.of(today, LocalTime.of(9, 30, 0), ZoneId.of("America/New_York"));
        ZonedDateTime XNYS_CLOSE_TIME = ZonedDateTime.of(today, LocalTime.of(16, 0, 0), ZoneId.of("America/New_York"));

        /* Hong Kong Stock Exchange opening and closing times: */
        ZonedDateTime XHKG_OPEN_TIME = ZonedDateTime.of(today, LocalTime.of(9, 30, 0), ZoneId.of("Asia/Hong_Kong"));
        ZonedDateTime XHKG_CLOSE_TIME = ZonedDateTime.of(today, LocalTime.of(16, 0, 0), ZoneId.of("Asia/Hong_Kong"));

        /* Saudi Stock Exchange opening and closing times: */
        ZonedDateTime XSAU_OPEN_TIME = ZonedDateTime.of(today, LocalTime.of(10, 0, 0), ZoneId.of("Asia/Riyadh"));
        ZonedDateTime XSAU_CLOSE_TIME = ZonedDateTime.of(today, LocalTime.of(15, 0, 0), ZoneId.of("Asia/Riyadh"));

        /* Australian Securities Exchange opening and closing times: */
        ZonedDateTime XASX_OPEN_TIME = ZonedDateTime.of(today, LocalTime.of(10, 0, 0), ZoneId.of("Australia/Sydney"));
        ZonedDateTime XASX_CLOSE_TIME = ZonedDateTime.of(today, LocalTime.of(16, 0, 0), ZoneId.of("Australia/Sydney"));


        MIC.put("XLON", Arrays.asList(XLON_OPEN_TIME, XLON_CLOSE_TIME, XLON_OPEN_AUCTION_TIME, XLON_CLOSE_AUCTION_TIME));
        MIC.put("XNYS", Arrays.asList(XNYS_OPEN_TIME, XNYS_CLOSE_TIME));
        MIC.put("XHKG", Arrays.asList(XHKG_OPEN_TIME, XHKG_CLOSE_TIME));
        MIC.put("XSAU", Arrays.asList(XSAU_OPEN_TIME, XSAU_CLOSE_TIME));
        MIC.put("XASX", Arrays.asList(XASX_OPEN_TIME, XASX_CLOSE_TIME));

        return MIC;
    }


    // To explore:
        // 1. Pass current time to method that return sif market is open or closed DONE
        // 2. returning phases of the market: you might need to add CLOSING AUCTION,OPENING AUCTION & OPEN& CLOSE TIMES DONE
        // 3. a problem arises when RETURNING market phases for different markets e.g., saudi is open sun-thurs not mon-fri like the others
        // 4. Perhaps




    // Notes from Mentor meetings 24 Oct
        /*
         - have a map of market open & close times (key: market ISOC codes? XLON , value: instance of market)
         - no need to make the market open time & close time static - you'd want to pass it in the constructor
         -  return type: good idea to consider returning market phases (opening auction, closing auction etc.) as an enum
            - so enum of isOpen, isClosed,
         - scope of regulation & checking prices : isolate the two concepts ??
        */


    /*
     New Method:
     takes in current time and market isoc code
     returns if market is open or closed
     OR STRETCH GOAL: the market phase and then another perhaps determine if you can trade based on which phase it is?
     Isoc code: store key (code) STRING & values: 2 - MARKET_OPEN_TIME & MARKET_CLOSE_TIME ZonedDateTime!
    */
}
