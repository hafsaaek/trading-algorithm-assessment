package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        /*
         */

        // Parametrs
        @SuppressWarnings("unused")
        long parentOrderQuantity = 300; // assume a client given parent order
        long quantity = 100; // fixed child order quantity, assume 1/3 of parent order
        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only
        int totalOrderCount = allChildOrders.size(); // total number of child orders created so far
        int activeOrderCount = activeChildOrders.size(); // number of active child orders

        // 1. Create a child order if total orders < 3
        if (totalOrderCount < 3 && state.getAskLevels() > 0) {
            logger.info("[MYALGO] Finding best bid before placing child order");
            final BidLevel bidPrice = state.getBidAt(0);
            long bestBid = bidPrice.price;

            logger.info("[MYALGO] Adding BID order for: " + quantity + "@" + bestBid + ": you now have a total of " + activeOrderCount + " active orders.");

            return new CreateChildOrder(Side.BUY, quantity, bestBid);
        }

        logger.info("[MYALGO] Current active child orders after creation: " + activeOrderCount);


        // 2. Cancel the first child order if there are 3 active orders and total order count is 3
        if (totalOrderCount == 3 && activeOrderCount == 3) {
            final var option = activeChildOrders.stream().findFirst();
            if (option.isPresent()) {
                var childOrder = option.get();
                logger.info("[MYALGO] Cancelling order: " + childOrder);
                return new CancelChildOrder(childOrder);
            }
        }
        logger.info("[MYALGO] Current active child orders after cancellation: " + activeOrderCount);


        // 3. End the program when 3 child orders have been created
        if (totalOrderCount == 3) {
            logger.info("[MYALGO] Total of 3 child orders created. No further action required.");
            return NoAction.NoAction;
        }
        logger.info("[MYALGO] Current active child orders after creating 3 total orders: " + activeOrderCount);


        logger.info("[MYALGO] No action taken.");
        return NoAction.NoAction;

    }
}