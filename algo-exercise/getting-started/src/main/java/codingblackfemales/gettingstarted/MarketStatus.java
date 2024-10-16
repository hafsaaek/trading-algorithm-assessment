package codingblackfemales.gettingstarted;

/**
 * Always good to use interfaces.
 */
public interface MarketStatus {
    /**
     * May make sense to call it isMarketOpen() and test for open rather than closed. Also in the general case I would
     * expect market to be passed as argument. Potentially also the current time should be an argument to allow back
     * testing with historical data.
     */
    boolean isMarketClosed();
}
