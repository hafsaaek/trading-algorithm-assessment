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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    /*** Basic Trading Algorithm Logic:
     * 1. Maintain 3 passive child orders on the market, each 100 shares, to fill a parent market order of 300 shares .
     * 2. Create new child orders if there are less than 3 orders on the market.
     * 3. Cancel the oldest active order when there are 3 or more active orders.
     * 4. Return NO Action if:
         * - Total filled quantity reaches 300 shares.
         * - More than 4 child orders (active + canceled) have been created.
         * - 3 (fully executed) child orders have fully filled.
     * 5. Over-Execution has been accounted for because we are ensuring:
         * - Total filled quantity of all Orders does not exceed the parent order quantity
         * - If 3 orders are fully filled, no more orders are created */

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MY-ALGO] The state of the order book is:\n{}", orderBookAsString);

        long parentOrderQuantity = 300; // assume a client given parent order
        long childOrderQuantity = 100; // fixed child order quantity, assume 1/3 of parent order
        int maxOrders = 4;
        int maxOrdersOnTheMarket = 3;
        BidLevel bestBid = state.getBidAt(0); // get best bid price (highest bid)

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only (non cancelled ones)
        List<ChildOrder> filledOrders = new ArrayList<>(); // to store  filled orders

        /*
         1. Prioritise retuning NO action to ensure the program checks when to stop before doing anything else
         1.1 Find filled active orders & deduce total filled quantity
        */
        for (ChildOrder activeChildOrder : activeChildOrders) {
            if (activeChildOrder.getFilledQuantity() == childOrderQuantity) {
                filledOrders.add(activeChildOrder);
            }
        }
        logger.info("[MY-ALGO] Filled Orders Count: {}", filledOrders.size());

        long totalFilledQuantity = allChildOrders.stream()
                .mapToLong(ChildOrder::getFilledQuantity)
                .sum(); // sum of quantities of all filled orders
        logger.info("[MY-ALGO] Total Filled Quantity for orders: {}", totalFilledQuantity);

        /* 1.2 Stop if total filled quantity meets the parent order quantity or max orders have been created */
        if (totalFilledQuantity >= parentOrderQuantity|| allChildOrders.size() >= maxOrders) {
            logger.info("[MY-ALGO] Total filled quantity has reached the target of {} or Maximum number of child orders created: {}. No more actions required.", totalFilledQuantity, allChildOrders.size());
            return NoAction;
        }

        /* 2. Ensure 3 active orders are on the market if none have been filled */
        int activeNonFilledOrders = activeChildOrders.size() - filledOrders.size();
        if (maxOrdersOnTheMarket > activeChildOrders.size()) {
            logger.info("[MY-ALGO] Creating new child order to maintain 3 active orders, want 3, have: {}", activeNonFilledOrders);
            long price = bestBid.price;
            return new CreateChildOrder(Side.BUY, childOrderQuantity, price);
        }

        /* 3. If there are more than 3 active orders & none of the 3 have been filled, cancel the oldest order */
        if (filledOrders.isEmpty() && activeNonFilledOrders >= 3) {
            ChildOrder nonFilledOrderToCancel = activeChildOrders.get(0); // stream & filter to active but not filled orders!
            logger.info("[MY-ALGO] Cancelling order: {}", nonFilledOrderToCancel);
            logger.info("[MY-ALGO] Order State: {}", nonFilledOrderToCancel.getState());
            return new CancelChildOrder(nonFilledOrderToCancel);
        }
        logger.info("[MY-ALGO] No action to take");
        return NoAction;
    }

}