package codingblackfemales.gettingstarted;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.pattern.Util;
import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;

public class StretchAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

    

        long quantity = 100; // fixed child order quantity, assume 1/3 of parent order
        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only
        int totalOrderCount = allChildOrders.size(); // total number of child orders created so far
        int activeOrderCount = activeChildOrders.size(); // number of active child orders

        final BidLevel bid = state.getBidAt(0);
        final AskLevel ask = state.getAskAt(0);

        long bestBid = bid.price;
        long bestAsk = ask.price;

        double vwap = calculateVWAP();

    
        if (bestBid < vwap) { // if price < vwap: buy
            logger.info("[ADDCANCELALGO] Adding BID order for" + quantity + "@" + bestBid + "You now have a total of " + activeChildOrders.size());
            return new CreateChildOrder(Side.BUY, quantity, bestBid);
        }else if (bestAsk > vwap) { // if price > vwap: sell so cancel the buy order 
         
        } else {
            logger.info("Do nothing for now until market stabilises?");
            return NoAction.NoAction;
        } 
    }
        private double calculateVWAP() {
        throw new UnsupportedOperationException("Unimplemented method 'calculateVWAP'");
        }
    }

   
    

