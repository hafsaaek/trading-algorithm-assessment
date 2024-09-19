package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.util.Util;
import messages.order.Side;

import static codingblackfemales.action.NoAction.NoAction;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        /********
        * Trading Algorithm Logic:
            * 1. Maintain 3 active child orders, each for 100 shares, to fill a parent order of 300 shares.
            * 2. Create new child orders if active orders are less than 3.
            * 3. Cancel the oldest active order when there are 3 or more active orders.
            * 4. Stop placing new orders if:
            *    - Total filled quantity reaches 300 shares.
            *    - More than 12 child orders (active + canceled) have been created.
            *    - 3 child orders have fully filled.
            * 5. Handle partial fills by adding their quantities to the total filled amount.         
        */

        long parentOrderQuantity = 300; // assume a client given parent order
        long childOrderQuantity = 100; // fixed child order quantity, assume 1/3 of parent order
        long targetBuyPrice = 10; // target buy price

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only (non cancelled ones)

        // 1. Prioritise retuning NO action to ensure the porgam checks when to stop before doing anything else
        long totalFilledQuantity = 0; // sum of quanitites of all filled orders
        List<ChildOrder> filledOrders= new ArrayList<>(); 

        for(ChildOrder activeChildOrder : activeChildOrders){
            if (activeChildOrder.getState() == OrderState.FILLED || activeChildOrder.getFilledQuantity() > 0) {
                logger.info("[MYALGO] Active Child order" + activeChildOrder + " has filled quanitity:" + activeChildOrder.getFilledQuantity());
                totalFilledQuantity += activeChildOrder.getFilledQuantity();
                filledOrders.add(activeChildOrder);
            }   
        }
        // Conditions to stop the program: if 3 orders have been fulled (assuming 3 sets of 100 quanitities) OR maxChildOrders has been created (arbitarty vlaue of 12) OR full parent order has been filled (300) 
        if(totalFilledQuantity >= parentOrderQuantity || allChildOrders.size() >= 12) {            
            return NoAction;
        }


         // 2. If there are active orders more than 3, cancel the oldest order 
        if (activeChildOrders.size() >= 3) {
            ChildOrder childOrderToCancel = activeChildOrders.get(0);
            logger.info("[MYALGO] Cancelling order: " + childOrderToCancel);
            logger.info("[MYALGO] Order States: " + childOrderToCancel.getState());
            return new CancelChildOrder(childOrderToCancel);
        }


        // 3. Now focus on cancelling child orders
        if (activeChildOrders.size() < 3 && totalFilledQuantity < parentOrderQuantity && state.getAskLevels() > 0) {
            logger.info("[MYALGO] Adding BID order for: " + childOrderQuantity + "@" + targetBuyPrice);
            return new CreateChildOrder(Side.BUY, childOrderQuantity, targetBuyPrice); 
        }

        // 4. Need to account for partially filled order when creating new orders!


        logger.info("[MYALGO] No action to take");
        return NoAction;
    }

}