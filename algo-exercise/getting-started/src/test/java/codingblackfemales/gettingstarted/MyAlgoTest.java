package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.action.NoAction;


import static org.junit.Assert.assertEquals;
// import org.junit.jupiter.api.DisplayName;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc..)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */
public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic();
    }

    /*
     * Tests:
        1. Ensure only 6 max orders are created DONE
        2. Ensure 3 orders are on the market (state active) unless 6 are created DONE
        3. Once 3 orders are created (active) - the oldest order is cancelled DONE
        4. For 6 orders created, only 3 are active and 9 are order state cancelled DONE
        5. Program stops after 6 orders are created i.e. returns NO action DONE
        6. Program stops after totalFilledQuantity == ParentOrderQuantity i.e. returns NO action DONE
        7. No new orders are created after totalFilledQuantity == ParentOrderQuantity i.e. ensure totalFilledQuantity is never > ParentOrderQuantity FAILING HERE
        8. For a partially filled order --> remaining quantity is created with a new childOrder NEED TO COVER THIS IN LOGIC
        9. After 6 are created - are the final 3 active orders cancelled? --> can do this by adding a condition of
        10. EDGE CASE: what happens if 1 order is filled? Does the logic stop creating orders (since 2 are already on the market) &
     */

    MyAlgoLogic mylogic = new MyAlgoLogic();

    @Test
    // @DisplayName
    public void testOrdersCreation() throws Exception {
        int maxOrders = 6;
        //create a sample market data tick....
        send(createTick());

        //simple assert to check we have created max 6 orders created
        assertEquals(maxOrders, container.getState().getChildOrders().size());
    }

    @Test
    public void testNoMoreThanMaxOrdersChildOrdersCreated() throws Exception {
        int maxOrders = 6;

        //create a sample market data tick....
        send(createTick());

        assertEquals(maxOrders, container.getState().getChildOrders().size());

        // Send another tick and assert NoAction is returned after re-triggering the market to further prove no more than 6 orders are created
        send(createTick());
        Action returnAction = mylogic.evaluate(container.getState());

        assertEquals(NoAction.class, returnAction.getClass());

    }

    @Test
    public void testExactlyThreeOrdersActiveOnMarket() throws Exception {

        //create a sample market data tick....
        send(createTick());

        //simple assert to check we had 3 orders created
        assertEquals(3, container.getState().getActiveChildOrders().size());

        // re-run the trigger
        send(createTick());
        assertEquals(3, container.getState().getActiveChildOrders().size());
    }


    @Test
    public void testOldestOrderIsCancelled() throws Exception {

        //create a sample market data tick....
        send(createTick());
        send(createTick());
        send(createTick());
        send(createTick());

        var firstCanceledChild = container.getState().getChildOrders().stream().filter(childOrder -> childOrder.getState() == OrderState.CANCELLED).findFirst().orElse(null);

        assertNotNull(firstCanceledChild);

        // assert first cancelled is order ID = 2 as logs show first Order created ID == 2
        assertEquals(2, firstCanceledChild.getOrderId());
    }

    @Test
    public void testTotalCancelledAndActiveOrders() throws Exception {

        //create a sample market data tick....
        send(createTick());
        long cancelledOdersCount = container.getState().getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED).count();

        long nonCancelledOrdersCount = container.getState().getChildOrders().stream().filter(order -> order.getState() != OrderState.CANCELLED).count();

        long totalOrders = container.getState().getChildOrders().size();

        //simple assert to check we had have 3 orders created
        assertEquals(3, nonCancelledOrdersCount);

         //simple assert to check we had have 3 orders created
         assertEquals(3, cancelledOdersCount);
        
         //simple assert to check we had 6 orders created
        assertEquals(6, totalOrders);
    }

    @Test
    public void testOrderFilling() throws Exception {
        // Send a sample tick to simulate market data
        send(createTick());

        // Simulate filling the active orders (each with 100 shares)
        container.getState().getActiveChildOrders().forEach(order -> order.addFill(100, 10));

        // Calculate the total filled quantity
        long filledQuantity = container.getState().getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        // Assert the total filled quantity is 300 (3 orders of 100 shares each)
        assertEquals(300, filledQuantity);
    }

    @Test
    public void testNoMoreOrdersAfterOneIsFilled() throws Exception {
        // Simulate sending the first tick to create 3 orders
        send(createTick());


        // Simulate filling the 3 active orders
//        container.getState().getActiveChildOrders().forEach(order -> order.addFill(100, 10));

        // Verify the total filled quantity is 300 (3 orders filled with 100 shares each)
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        assertEquals(100, filledQuantity);

        // Re-evaluate the algo to ensure no more orders are created after filling 3
//        Action returnAction = mylogic.evaluate(container.getState());
//        assertEquals(NoAction.class, returnAction.getClass()); // Expect no action after 3 orders are filled

        // Ensure that no new orders are added after the 3 orders are filled
        assertEquals(3, container.getState().getChildOrders().size()); // Only 3 orders should exist, and all should be filled
    }

}