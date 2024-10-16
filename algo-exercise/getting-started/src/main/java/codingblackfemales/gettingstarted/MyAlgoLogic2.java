package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import static codingblackfemales.action.NoAction.NoAction;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic2 implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic2.class);

    private final MarketStatus marketStatus;

    public MyAlgoLogic2(MarketStatus marketStatus) {
        this.marketStatus = marketStatus;
    }

    @Override
    public Action evaluate(SimpleAlgoState state) {

        /* New logic:
            * 1. Maintain 3 active child orders on the market, each for 100 shares, to fill a parent order of 300 shares
            * 2. Cancel an order if the order is active but not filled and market is closed (time based on LSEG opening times)
            * 3.  Stop the script i.e. return No Action when:
                * - Total filled quantity reaches parent order or 3 child orders have fully filled to prevent over-execution
                * - Three child orders (active + canceled) have been created
                * None of the above conditions or 1 & 2 are true */

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[STRETCH-ALGO] The state of the order book is:\n" + orderBookAsString);

        long parentOrderQuantity = 300; // assume a client given parent order
        long childOrderQuantity = 100; // fixed child order quantity, assume 1/3 of parent order
        BidLevel bestBid = state.getBidAt(0);
        int maxOrders = 3;

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only (non cancelled ones)
        List<ChildOrder> filledOrders = new ArrayList<>(); // to store  filled cancelled orders

        // 1.1 Find filled active orders & deduce total filled quantity & Cancel non-filled orders if the market is closed
        boolean marketClosed = marketStatus.isMarketClosed();
        for (ChildOrder activeChildOrder : activeChildOrders) {
            if (activeChildOrder.getFilledQuantity() == childOrderQuantity) { // if my child order of 100 is
                filledOrders.add(activeChildOrder);
            }  // 1.2 If an active is not filled by the time the market closes - cancel it
            if (!filledOrders.contains(activeChildOrder) && marketClosed && !activeChildOrders.isEmpty()) {
                logger.info("[STRETCH-ALGO] The market is closed, cancelling orders ");
                logger.info("[STRETCH-ALGO] Cancelling day order: {}", activeChildOrder);
                logger.info("[STRETCH-ALGO] Order State: {}", activeChildOrder.getState());
                return new CancelChildOrder(activeChildOrder);
            }
        }
        logger.info("[STRETCH-ALGO] Filled Orders Count: {}", filledOrders.size()); // using + concatenation will use memory to log info we don't need to call yet

        int activeNonFilledOrders = activeChildOrders.size() - filledOrders.size(); // to store  non-filled cancelled orders

        long totalFilledQuantity = allChildOrders.stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of quantities of all filled orders
        logger.info("[STRETCH-ALGO] Total Filled Quantity for orders: {}", totalFilledQuantity);

        // 2 Stop if total filled quantity meets the parent order quantity and there are 3 fully filled orders
        if (totalFilledQuantity >= parentOrderQuantity && filledOrders.size() >= 3) {
            logger.info("[STRETCH-ALGO] Total filled quantity has reached the target of {}. No more actions required.", totalFilledQuantity);
            return NoAction;
        }

        // 3 Stop if we've reached the max number of child orders (active + cancelled) or the market is closed
        if (allChildOrders.size() >= maxOrders || marketClosed) {
            logger.info("[STRETCH-ALGO] Maximum number of child orders created: [{}] or market is closed: [{}]", allChildOrders.size(), marketClosed);
            return NoAction;
        }

        // 4. Ensure 3 active orders are on the market
        if (filledOrders.size() < 3 && activeChildOrders.size() < 3 && totalFilledQuantity < parentOrderQuantity) {
            logger.info("[STRETCH-ALGO] Creating new child order to maintain 3 active orders, want 3, have: {}", activeNonFilledOrders);
            long price = bestBid.price;
            return new CreateChildOrder(Side.BUY, childOrderQuantity, price);
        }

        // 5. Need to account for partially filled order when creating new orders!
        // perhaps use the old logic that you create new orders with 100 - old order placed on the market to ensure that 100 is always on the market

        logger.info("[STRETCH-ALGO] No action to take");
        return NoAction;
    }

}