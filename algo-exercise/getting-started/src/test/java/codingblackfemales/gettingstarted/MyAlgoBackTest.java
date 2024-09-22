package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * This test plugs together all the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 * If you cross the spread (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object).
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
        var state = container.getState();
        assertEquals(0, state.getActiveChildOrders().size());
        assertEquals(0, state.getChildOrders().size());

        // Sending first tick should trigger no matches
        send(createTick());
        state = container.getState();
        long cancelledOrderCount = state.getChildOrders().size() - state.getActiveChildOrders().size();
        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        long filledOrdersCount = state.getActiveChildOrders().stream().filter(order -> order.getFilledQuantity() == 100).count();

        //ADD asserts to check that sending first tick will trigger 6 orders in total to be created, 3 cancellations and zero fills
        assertEquals(4, state.getChildOrders().size());
        assertEquals(3, state.getActiveChildOrders().size());
        assertEquals(1, cancelledOrderCount);
        assertEquals(0, filledQuantity);
        assertEquals(0, filledOrdersCount);
    }

    @Test
    public void testOneMatchResponse() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // Log and verify initial conditions
        var state = container.getState();
        long filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();
        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();


        // Initial conditions check
        assertEquals(0, filledOrdersCount);
        assertTrue(filledQuantity == 0);

        // Send second tick and verify the logic's reaction when the market moves towards us
        send(createTick2()); // Ensures one order is filled
        state = container.getState();
        filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(1, filledOrdersCount); // One order should be filled
        assertEquals(100, filledQuantity);
        assertEquals(4, state.getChildOrders().size()); // Total child orders remain 6
        assertEquals(3, state.getActiveChildOrders().size()); // Total child orders remain 6
    }

    @Test
    public void testTwoMatchesResponse() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // Send third tick and verify the logic's reaction when the market has an ask that can offer us 2 offers
        send(createTick3()); // Ensures one order is filled
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(2, filledOrdersCount); // Two orders should be filled
        assertEquals(200, filledQuantity);
    }

    @Test
    public void testThreeMatchesResponse() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // Send third tick and verify the logic's reaction when the market has an ask that can offer us 2 offers
        send(createTick4()); // Ensures one order is filled

        long parentOrderQuantity = 300;
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(3, filledOrdersCount); // Two orders should be filled
        assertEquals(parentOrderQuantity, filledQuantity);
    }

    @Test
    public void testOverExecution() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // To test that if another offer with a 100 or 400
        send(finalMarketTickOverExecution());
        long parentOrderQuantity = 300;
        var state = container.getState();
        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getActiveChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(3, filledOrdersCount); // Two orders should be filled
        assertEquals(parentOrderQuantity, filledQuantity);

        // re-trigger market data to ensure no more orders are created and over-execution is avoided
        send(finalMarketTickOverExecution());
        filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        assertEquals(parentOrderQuantity, filledQuantity);

    }

}
