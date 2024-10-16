package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(0, filledQuantity);

        // Send second tick and verify the logic's reaction when the market moves towards us
        send(createTick2()); // Ensures one order is filled
        state = container.getState();
        filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(1, filledOrdersCount); // One order should be filled
        assertEquals(100, filledQuantity); // 100 shares have been bought assertion
        assertEquals(4, state.getChildOrders().size()); // Total child orders remain 6
        assertEquals(3, state.getActiveChildOrders().size()); // Total child orders remain 6
    }

    @Test
    public void testTwoMatchesResponse() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // Send third tick and verify the logic's reaction when the market has an ask that can offer us 2 offers
        send(createTick3()); // Ensures two orders are filled
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(2, filledOrdersCount); // Two orders should be filled
        assertEquals(200, filledQuantity); // 200 shares have been bought assertion
    }

    @Test
    public void testThreeMatchesResponse() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // Send fourth tick and verify the logic's reaction when the market has an ask that can offer us 3 offers & thus meet parent order
        send(createTick4()); // Ensures one order is filled

        long parentOrderQuantity = 300;
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(3, filledOrdersCount); // Three orders should be filled
        assertEquals(parentOrderQuantity, filledQuantity); // All 300 shares (parent order) have been bought assertion
    }

    @Test
    public void testOverExecution() throws Exception {
        // Sending first tick should trigger no matches
        send(createTick()); // Creates the initial conditions with no fills

        // To test that if more than 3 orders (max fill-able child orders) is possible
        send(finalMarketTickOverExecution());
        long parentOrderQuantity = 300;
        var state = container.getState();
        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        long filledOrdersCount = state.getActiveChildOrders().stream()
                .filter(order -> order.getFilledQuantity() == 100).count();

        // Check the final state
        assertEquals(3, filledOrdersCount); // Three orders should be filled
        assertEquals(parentOrderQuantity, filledQuantity); // Only 300 shares (parent order) should be bought assertion

        // re-trigger market data to ensure no more orders are created and over-execution is avoided
        send(finalMarketTickOverExecution());
        filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
        assertTrue(filledQuantity != 400);
        assertEquals(300, filledQuantity);

    }

}
