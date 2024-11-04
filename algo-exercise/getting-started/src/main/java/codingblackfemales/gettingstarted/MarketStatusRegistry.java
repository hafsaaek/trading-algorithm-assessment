package codingblackfemales.gettingstarted;

public interface MarketStatusRegistry {

    default MarketStatus getByMIC(String marketIdentifierCode) {
        return null;
    };
}

// first get the market status by defining the market you want to trade in
