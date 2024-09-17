package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.util.Util;
import messages.order.Side;

import static codingblackfemales.action.NoAction.NoAction;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        // Parametrs
        long parentOrderQuantity = 300; // assume a client given parent order
        long childOrderQuantity = 100; // fixed child order quantity, assume 1/3 of parent order
        long targetExecutionPrice = 10; // target buy price
        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only (non cancelled ones)
        long placedQuantity = 0; // sum of all active orders quantity on the market 
        long totalFilledQuantity = 0; // sum of quanitites of all filled orders

        // 1. Cancel an order once it's entire quantty (100) has been executed/filled
        for (ChildOrder activeChildOrder : activeChildOrders) {
            if (activeChildOrder.getState() == OrderState.FILLED || activeChildOrder.getFilledQuantity() > 0) {
                logger.info("[MYALGO] Cancelling order:" + activeChildOrder);
                return new CancelChildOrder(activeChildOrder);
            } 
        }

        // 2. Switch statment for cumaltively adding up totalFilledQuantity & amomunt of quantity on the market for each child order
        for (ChildOrder childOrder : allChildOrders) {
            totalFilledQuantity += childOrder.getFilledQuantity();
            if (childOrder.getState() == OrderState.ACKED || childOrder.getState() == OrderState.PENDING) {
                placedQuantity += childOrder.getQuantity() - childOrder.getFilledQuantity();
            }
        }
        logger.info("[MYALGO] Total filled quantity so far: " + totalFilledQuantity);
        logger.info("[MYALGO] Currently placed quantity in the market: " + placedQuantity);
    

        // 3. Create a new order once a child order's full quanityt has been executed, check if the child order's full quantity has been filled first 
        if(totalFilledQuantity < parentOrderQuantity)  {
            long quanitityLeftToPlace = parentOrderQuantity - totalFilledQuantity; // determines if a new order can be created by checking what has been executed from the original child order quantity
            if (quanitityLeftToPlace > 0 && activeChildOrders.isEmpty()) { 
                long newChildOrderQuantity = Math.min(childOrderQuantity, quanitityLeftToPlace);  // Ensure no over-execution
                // nothign for us to do 
                logger.info("[MYALGO] Adding BID order for: " + newChildOrderQuantity + "@" + targetExecutionPrice);
                return new CreateChildOrder(Side.BUY, newChildOrderQuantity, targetExecutionPrice); // Create a full new child order or a partial 
            } 
        }

        logger.info("[MYALGO] No action to take");
        return NoAction;
    }

    // logger.debug(); // for debugging
    
}


// Testing:
    // test for quantity <= 300 - never over 300 --> over execution
    // Cancel logic assumes a child order is filled when all of it's quanitities are filled - test this later! How to account for an order state == filled if ithe order is only partially filled 
    // Logic flaw: how do we check that the execution price of riendly for partially filled orders??

// Logic summarised:
    // 1.  