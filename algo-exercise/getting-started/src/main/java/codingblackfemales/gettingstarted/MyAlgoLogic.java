package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;

import static codingblackfemales.action.NoAction.NoAction;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class); // to track events, errors, or important information specifically within the MyAlgoLogic class

    /*
        You need to account for 3 main functions:
            1. Add new orders (queuing according to price-time-priority?)
                --> Strategy: create orders passively but sell if price > vwap and buy if price < vwap
            2. Execute and match bids to asks & vice versa by manually injecting a market price to provide an offer Maybe this is for stretch exercise?
            3.  Cancel orders once executed

        Stretch exercise: make money. How?
        --> by looking at more than one market data e.g., have an array of market prices you can compare with your bid/ask price and then select the best price from that
        */
    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state); // create order book

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        long parentOrderQuantity = 3000; // assume a client given parent order
        long quantity = 1000; // fixed child order quantity, assume 10% of parent order
        int totalRequiredChildOrders = (int) (parentOrderQuantity / quantity); // number of required child orders to fill parent order
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // number of active child orders
        int remainingOrdersNeeded = totalRequiredChildOrders - state.getChildOrders().size(); // to keep track of child orders made

        // 1. If a child order in the list of active orders is filled - then cancel it
        for (ChildOrder childOrder : activeChildOrders) {
            if (childOrder.getState() == OrderState.FILLED) {
                logger.info("[MYALGO] Cancelling order:" + childOrder);
                return new CancelChildOrder(childOrder);
            }
        }
        logger.info("[MYALGO] Current active child orders1: " + activeChildOrders.size());



        // 2. If there are enough child orders made - return no action
        if (activeChildOrders.size() >= totalRequiredChildOrders) {
            logger.info("[MYALGO] All child orders have been created for parent order");
            return NoAction;
        }

        logger.info("[MYALGO] Current active child orders2: " + activeChildOrders.size());


        // 3. (Will run regardless of 1 & 2) Create a new child order if there is 1 or more ask offers 
        if (remainingOrdersNeeded > 0 && state.getAskLevels() > 0) { 
            logger.info("[MYALGO] Sell order found, finding best ask before placing child order");
            final BidLevel bidPrice = state.getBidAt(0);
            long bestBid = bidPrice.price; // // highest buy price - bid price will almost always be lower than the ask or “offer,” price!

            // If there are missing child orders to fill parent order - create new child order

            logger.info("[MYALGO] Adding BID order for: " + quantity + "@" + bestBid + ": you now have a total of " + activeChildOrders.size() + " and require " + remainingOrdersNeeded + " more child orders to fill parent order");
            return new CreateChildOrder(Side.BUY, quantity, bestBid);
        } 

        logger.info("[MYALGO] Current active child orders3: " + activeChildOrders.size());

        return NoAction;

    } // grep -r askLevels 
    // ./mvnw clean test --projects algo-exercise/getting-started -Dtest=codingblackfemales.gettingstarted.MyAlgoTest > test-results.txt

}

/*
OLD LOGIC: use VWAP to judge when to sell and buy 
        double vwap = calculateVWAP();

 if (bestBid < vwap) { // if price < vwap: buy
            logger.info("[ADDCANCELALGO] Adding BID order for" + quantity + "@" + bestBid + "You now have a total of " + activeChildOrders.size() + " and require " + remainingOrdersNeeded + " more child orders to fill parent order" );
            return new CreateChildOrder(Side.BUY, quantity, bestBid);
        }else if (bestAsk > vwap) { // if price > vwap: sell so cancel the buy order 
            var childOrder = option.get();
            logger.info("[ADDCANCELALGO] Cancelling order:" + childOrder);
            return new CancelChildOrder(childOrder);            
            // The other option is to change the condition to SellPrice > vwap; thus create a sell child order but maybe this is where I need to cancel the order 
            // logger.info("[ADDCANCELALGO] Adding ASK order for" + quantity + "@" + bestAsk + "You now have a total of " + activeChildOrders + " and require " + remainingOrdersNeeded + " more child orders to fill parent order");
            // return new CreateChildOrder(Side.SELL, quantity, bestAsk);            
        } else {
            logger.info("Do nothing for now until price drops");
            return NoAction.NoAction;
        } 
        private double calculateVWAP() {
        // throw new UnsupportedOperationException("Unimplemented method 'calculateVWAP'");
        return 100;
    }*/