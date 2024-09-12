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

        /*
         */

        // Parametrs
        @SuppressWarnings("unused")
        long parentOrderQuantity = 300; // assume a client given parent order
        long quantity = 100; // fixed child order quantity, assume 1/3 of parent order
        long targetExecutionPrice = 10;
        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // active child orders only
        int totalOrderCount = allChildOrders.size(); // total number of child orders created so far
        int activeOrderCount = activeChildOrders.size(); // number of active child orders


        // our goal is to cancel filled ones - what's important - cancel what you have executed or send an order to get ahead of the market

        // cancel it once it's executed to make it more streamlined

        for (ChildOrder activeChildOrder : activeChildOrders) {
            if (activeChildOrder.getState() == OrderState.FILLED) { 
                return new CancelChildOrder(activeChildOrder);
            } // this assumes a child order is filled when all of it's quanitities are filled - test this later
        }
        long placedQuanity = 0; // sum of all active orders on the market 
        long totalFilledQuantity = 0; // sum of quanitites of all filled orders

        // consider switch statments

        for (ChildOrder childOrder: allChildOrders) {
            if(childOrder.getState() == OrderState.FILLED){
                totalFilledQuantity += childOrder.getFilledQuantity(); // how do we check for partially filled orders
                placedQuanity = placedQuanity + childOrder.getQuantity() - childOrder.getFilledQuantity(); // initial + original placed quanityt - how much was filled; 
            }
            if (childOrder.getState() == OrderState.CANCELLED) {
                // a cancelled order might have filled quanitity e.g., if an order was partially executed for 50 and then cancelled for whatever reason
                totalFilledQuantity += childOrder.getFilledQuantity(); // how do we check for partially filled orders
                // Order is no longer on the market so no need to accoutn for placed quanityt on the market
            } 
            if (childOrder.getState() == OrderState.ACKED) { // we're potetnially going to execute it - it's a full passive order or a partial order that needs to be executed
                totalFilledQuantity += childOrder.getFilledQuantity(); // how do we check for partially filled orders
                placedQuanity = placedQuanity + childOrder.getQuantity() - childOrder.getFilledQuantity(); // initial + original placed quanityt - how much was filled; 
            } 
            if (childOrder.getState() == OrderState.PENDING) { // pending should have no filled quantity in theory - we can still use the same formulas - pending in theory means it hasn't been acknowledged yet 
                totalFilledQuantity += childOrder.getFilledQuantity(); // covers the case should a pending order have a filled Quantity 
                placedQuanity = placedQuanity + childOrder.getQuantity() - childOrder.getFilledQuantity(); // initial + original placed quanityt - how much was filled; 

            // while in pending - we will not recieve executions
            }
        }

        // Next stop: if have not filled the total parent order quantity - and we don't have 
// test for quantity <= 300 - never over 300 --> over execution
        if(totalFilledQuantity < 300) {
            long quanitityLeftToPlace = 100 - placedQuanity;
            if (quanitityLeftToPlace > 0) { // everythign we don't have filled is in the market
                // nothign for us to do 
                return new CreateChildOrder(Side.BUY, quanitityLeftToPlace, targetExecutionPrice); // so we send an order for any partially filled quanitities of child order x
            } // this also returns a new child order for the 2nd, 3rd orders we don't need  
            // when a new order is created quanitityLeftToPlace = childorder.getQuantity
            // at this point - we have either executed
            // otherwise: we place an order on the market ideally 100
            // if we have 10 left of the previous order, do we want to send 100 or fill the partial remain order 
            // if you want to do the latter:             long quanitityLeftToPlace = 100 - parentOrderQuantity - totalFilledQuantity;

            // more elegant to ensure partial filled order remainders are filled
        }

        // Goal: execute 300, wih each Q = 100, even if they're partially filled orders - we have knowledge of this to send partially unfilled orders 
        // 1. send an order of 100:
            // 1. see if it has been filled by checking executedQuantity - condition been is executedQuanityt == 100? - if not - think about partially fille dorders later
            logger.info("[MYALGO] No action taken.");
            return NoAction.NoAction;
        }
    
}

//  Simpler strategy: ensure there are always 3 orders on the market and cancel (once it's executed). thus active == pending, non active == cancelled

// 1. place an roder of 100 
// 2. check can I trade ? do we have remaiing quanitties to fill 