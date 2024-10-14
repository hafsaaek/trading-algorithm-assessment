package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StretchAlgoBackTest extends AbstractAlgoBackTest {
    private MarketStatus marketStatus;
    @Override
    public AlgoLogic createAlgoLogic() {
        return new StretchAlgoLogic(marketStatus);
    }
    private StretchAlgoLogic logicInstance;
    private StretchAlgoLogic marketIsForcedOpenInstance;
    private StretchAlgoLogic marketIsForcedClosedInstance;

    @BeforeEach
    public void setUp(){
        marketStatus = new MarketStatus();
        logicInstance = new StretchAlgoLogic(marketStatus);
        MarketStatus marketIsForcedOpen = new MarketStatus() {
            @Override
            public boolean isMarketClosed() {
                return false;  // Force open
            }
        };

        MarketStatus marketIsForcedClosed = new MarketStatus() {
            @Override
            public boolean isMarketClosed() {
                return true;  // Force closed
            }
        };
        marketIsForcedOpenInstance = new StretchAlgoLogic(marketIsForcedOpen);
        marketIsForcedClosedInstance = new StretchAlgoLogic(marketIsForcedClosed);
    }

    // Test 1: Check isMarketClosed behaves as expected
    @Test
    public void testIsMarketClosedFunction() throws Exception{
        // Check the function returns true when the market is closed in real time and false when it's open in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 00);
        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());
        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertTrue(isMarketClosedTestVariable == logicInstance.isMarketClosed());

        // Also check that the market instances for OPEN or CLOSED used throughout tests are working as expected
        assertFalse(marketIsForcedOpenInstance.isMarketClosed()); // returns false for forced open instance
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // returns true for forced closed instance
    }

    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // 1. check no orders are on the market before triggering logic container
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 2. check no orders are created if the market is closed
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        send(createTick0());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        Assert.assertEquals(NoAction.class, returnAction.getClass()); // assert 'No action' action is returned

        // 3. Test that if the market is forced Open and enough data is collected on market trends where trend is stable (no overall or minimal change) - zero orders are created
        container.setLogic(marketIsForcedOpenInstance);
        assertTrue(!marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 4. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));
        assertFalse(container.getState().getActiveChildOrders().stream().anyMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
        long expectedChildOrderQuantity = 100;
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // asser quantity is
        long expectedBidPrice = container.getState().getBidAt(0).price;
        System.out.println("price is: " + expectedBidPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedBidPrice)); //


        // 5. Assert that the average calculator methods works as expected for order books (basic tick and BUY tick)
        List<StretchAlgoLogic.OrderBookLevel> orderArray = new ArrayList<StretchAlgoLogic.OrderBookLevel>();
        StretchAlgoLogic.OrderBookLevel order1 = new StretchAlgoLogic.OrderBookLevel(100, 100);
        StretchAlgoLogic.OrderBookLevel order2 = new StretchAlgoLogic.OrderBookLevel(90, 200);
        StretchAlgoLogic.OrderBookLevel order3 = new StretchAlgoLogic.OrderBookLevel(80, 300);
        orderArray.addAll(Arrays.asList(order1, order2 ,order3));
        double movingWeightAverage = Math.abs(logicInstance.calculateMovingWeightAverage(orderArray));
        System.out.println("calculated weight average is: " + movingWeightAverage );
        Assert.assertEquals(86.67, movingWeightAverage, 0.01);

        // 6. Assert that the average trend evaluation using list of averages is working as expected
        List<Double> listOfAverages = Arrays.asList(90.0, 91.0, 92.0, 93.0, 94.0, 95.0);
        Assert.assertEquals(5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);

        // 7. Assert no more orders are created if the trend changes e.g., it's more favourable to SELL now (we don't have stocks to sell yet, should still BUY)
        send(createTickSELLHigh());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().size() == 3);

        // 8. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertTrue(container.getState().getChildOrders().size() == 3); // to assert no active orders but there are 3 CANCELLED orders
    }

    @Test
    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

    @Test
    public void testBUYCondition() throws Exception {
        // 1. check no orders are on the market before triggering logic container
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created
        container.setLogic(marketIsForcedOpenInstance);
        assertTrue(!marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));
        assertFalse(container.getState().getActiveChildOrders().stream().anyMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
        long expectedChildOrderQuantity = 100;
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // asser quantity is
        long expectedBidPrice = container.getState().getBidAt(0).price;
        System.out.println("Placed 3 ask orders with bid price: " + expectedBidPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedBidPrice)); //

        // 3. Check orders are filled when the right opportunity presents itself
        send(createTickFillBUYOrders());
        //then: get the state
        var state = container.getState();
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

        // 4. Assert no more orders are created if the trend changes e.g., it's more favourable to SELL now (we don't have stocks to sell yet, should still BUY)
        send(createTickSELLHigh());
        send(createTickBUYLow());
        send(createTickSELLHigh());
        assertTrue(container.getState().getChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().size() == 3);

        // 5. test these FILLED orders are NOT cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().size() == 3); // to assert filled active orders are NOT CANCELLED
    }

    @Test
    public void testSellCondition() throws Exception {
        // 1. check no orders are on the market before triggering logic container
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created
        container.setLogic(marketIsForcedOpenInstance);
        assertTrue(!marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickSELLHigh());
        assertTrue(container.getState().getActiveChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
        assertFalse(container.getState().getActiveChildOrders().stream().anyMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));

        long expectedChildOrderQuantity = 100;
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // asser quantity is
        long expectedAskPrice = container.getState().getAskAt(0).price;
        System.out.println("Placed 3 ask orders with ask price: " + expectedAskPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedAskPrice)); //

        // 3. Check orders are filled when the right opportunity presents itself
        send(createTickFillSELLOrders());
        //then: get the state
        var state = container.getState();
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

        // 4. Assert no more orders are created if the trend changes e.g., it's more favourable to SELL now (we don't have stocks to sell yet, should still BUY)
        send(createTickSELLHigh());
        send(createTickBUYLow());
        send(createTickSELLHigh());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().size() == 3);

        // 5. test these FILLED orders are NOT cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().size() == 3); // to assert filled active orders are NOT CANCELLED

        // 6. Profit made after BUYING at 96 as per testBUYCondition, the profit is:
        System.out.println("Profit made is: " + (expectedAskPrice - 96));
    }


}