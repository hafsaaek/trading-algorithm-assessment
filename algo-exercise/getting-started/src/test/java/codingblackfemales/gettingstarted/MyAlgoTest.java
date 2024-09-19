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
        1. Ensure only 12 max orders are created DONE
        2. Ensure 3 orders are on the market (state active) unless 12 are created DONE
        3. Once 3 orders are created (active) - the oldest order is cancelled DONE
        4. For 12 orders created, only 3 are acive and 9 are order state cancelled DONE
        5. Program stops after 12 orders are created i.e. returns NO action DONE
        6. Program stops after totalFilledQuantitity == ParentOrderQuantity i.e. returns NO action 
        7. No new orders are created after totalFilledQuantitity == ParentOrderQuantity i.e. ensure totalFilledQuantitity is never > ParentOrderQuantity
        8. For a partially filled order --> remaining quantity is created with a new childOrder
        9. After 12 are created - remaining 3 active orders are cancelled?
     */

    MyAlgoLogic mylogic = new MyAlgoLogic();

    @Test
    // @DisplayName
    public void testOrdersCreation() throws Exception {
        int maxOrders = 12;
        //create a sample market data tick....
        send(createTick());

        //simple assert to check we have created max 12 orders created
        assertEquals(maxOrders, container.getState().getChildOrders().size());
    }

    @Test
    public void testNoMoreThanTwelveChildOrdersCreated() throws Exception {
        for (int i = 0; i < 12; i++) {
            send(createTick()); 
        }

        assertEquals(12, container.getState().getChildOrders().size());

        // Send another tick and assert NoAction is returned after re-triggering the market to further prove no more than 12 orders are created
        send(createTick());
        Action returnAction = mylogic.evaluate(container.getState());

        assertEquals(NoAction.class, returnAction.getClass());

    }

    @Test
    public void testExactlyThreeOrdersActiveOnMarket() throws Exception {

        //create a sample market data tick....
        send(createTick());
        send(createTick());
        send(createTick());

        //simple assert to check we had 3 orders created
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
        for (int i = 0; i < 12; i++) {
            send(createTick());
        }

        long cancelledOdersCount = container.getState().getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED).count();

        long nonCancelledOrdersCount = container.getState().getChildOrders().stream().filter(order -> order.getState() != OrderState.CANCELLED).count();

        long totalOrders = container.getState().getChildOrders().size();

        //simple assert to check we had have 3 orders created
        assertEquals(3, nonCancelledOrdersCount);

         //simple assert to check we had have 3 orders created
         assertEquals(9, cancelledOdersCount);
        
         //simple assert to check we had 12 orders created
        assertEquals(12, totalOrders);
    }

    @Test
    public void testForOverExecution() throws Exception {
      
        //create a sample market data tick....
        // for (int i = 0; i < 3; i++) {
        //     send(createTick());
        // }
        send(createTick()); 


        // somehow calculate the total filled quantity if 3 orders are filled 
        container.getState().getActiveChildOrders().stream().forEach(order -> order.addFill(100, 10));
        long filledQuantity = container.getState().getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of filled quantity for all orders
        Action returnActionOverFilling = mylogic.evaluate(container.getState());


        // Send another tick and assert no more orders are created & NoActio is returned
        send(createTick()); 
        assertEquals(NoAction.class, returnActionOverFilling.getClass());
        assertEquals(300, filledQuantity);
        assertEquals(3, container.getState().getChildOrders().size());  // Ensure no more than 12 orders in total
    }

}