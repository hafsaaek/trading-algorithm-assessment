package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;

//import static codingblackfemales.gettingstarted.MyAlgoLogic.logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import codingblackfemales.sotw.OrderState;
import org.junit.Test;

/**
 * This test plugs together all of the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 *
 * If you cross the srpead (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object.
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 *
 */
public class MyAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testExampleBackTest() throws Exception {
        var zeroState = container.getState();
        assertEquals(0, zeroState.getActiveChildOrders().size());
        assertEquals(0, zeroState.getChildOrders().size());

        //create a sample market data tick....
        send(createTick());
        var state = container.getState();

        //ADD asserts when you have implemented your algo logic
        assertEquals(6, state.getChildOrders().size());
        assertEquals(3, state.getActiveChildOrders().size());
        assertEquals(100, state.getActiveChildOrders().get(0).getQuantity());
    }

    @Test
    public void testOrderFilling() throws Exception {
        send(createTick());
        var state = container.getState();

        // Verify the total filled quantity is 100 (3 orders filled with 100 shares each)
        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        //Check things like filled quantity, cancelled order count etc....
        assertEquals(0, filledQuantity);
    }

    @Test
    public void testOrderFilling2() throws Exception {
        // Send the first tick and check initial state
        send(createTick());

        // Log and verify initial conditions
        var state = container.getState();
        long filledOrdersCount = state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() == 100).count();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        assertEquals(0, filledOrdersCount);
        assertTrue(filledQuantity == 0);

        // Send second tick and verify the logic's reaction when the market moves towards us
        send(createTick2()); // Contains

        // Re-check state
        state = container.getState();
        filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        filledOrdersCount = state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() == 100).count();
        assertEquals(1, filledOrdersCount);
        assertTrue(filledQuantity >= 100);  // Ensure that at least 100 shares were filled

        assertEquals(6, state.getChildOrders().size());

    }

    @Test
    public void testOrderFilling3() throws Exception {

        // Send second tick and verify the logic's reaction when the market moves towards us
        send(createTick2()); // Contains

        // Re-check state
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        long filledOrdersCount = state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() == 100).count();
        assertEquals(1, filledOrdersCount);
        assertTrue(filledQuantity >= 100);  // Ensure that at least 100 shares were filled

//        assertEquals(1, state.getChildOrders().size());

    }

}
