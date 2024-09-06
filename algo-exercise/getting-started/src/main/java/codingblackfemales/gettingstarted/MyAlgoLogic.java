package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AddCancelAlgoLogic;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.marketdata.impl.SimpleFileMarketDataProvider;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class); // to track events, errors, or important information specifically within the MyAlgoLogic class

    AddCancelAlgoLogic addCancelAlgoLogic = new AddCancelAlgoLogic();
    SimpleFileMarketDataProvider marketData = new SimpleFileMarketDataProvider("src/main/resources/marketdata.txt");

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        /********
         *
         * Add your logic here....
         *
         */

        // You need to account for 3 main functions: 1. Add new orders (queuing according to price-time-priority), 2. Execute and match bids to asks & viceversa by manually injecting a market price to provide an offer 3.  Cancel orders once executed  
        // Stretch exercise: make money. How? --> by looking at more than one market data e.g., have an array of market prices you can copmare with your bid/ask price and then select the best price from that 

        if (state.getActiveChildOrders().size() < 20) { // should you use active (filter out cancelled) or getChildOrders 
            addCancelAlgoLogic.evaluate(state); 
            // Logic for executing the order - match it with a manually input ask order
            // need to find where the market data is kept or how to manually add it - hwo would this affect existing codebase - is there an easier way to do this?
        }

        return NoAction.NoAction;
    }
}
