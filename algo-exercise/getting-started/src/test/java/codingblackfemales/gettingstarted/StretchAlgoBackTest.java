package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StretchAlgoBackTest extends AbstractAlgoBackTest {
    private OrderBookService orderBookService;
    private MarketStatus marketStatus;
    private MovingWeightAverageCalculator mwaCalculator;
    private StretchAlgoLogic logicInstance;
    private StretchAlgoLogic marketIsForcedOpenInstance;
    private StretchAlgoLogic marketIsForcedClosedInstance;

    @Override
    public AlgoLogic createAlgoLogic() {
        return new StretchAlgoLogic(marketStatus,orderBookService, mwaCalculator);
    }

    @BeforeEach
    public void setUp(){
        orderBookService = new OrderBookService();
        mwaCalculator = new MovingWeightAverageCalculator(orderBookService);
        marketStatus = new MarketStatus();
        logicInstance = new StretchAlgoLogic(marketStatus, orderBookService, mwaCalculator);
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
        marketIsForcedOpenInstance = new StretchAlgoLogic(marketIsForcedOpen, orderBookService, mwaCalculator);
        marketIsForcedClosedInstance = new StretchAlgoLogic(marketIsForcedClosed, orderBookService, mwaCalculator);
    }

    @Test
    public void testBuyAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        Assertions.assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created */
        container.setLogic(marketIsForcedOpenInstance);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickBUYLow());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));

        long expectedChildOrderQuantity = 100; // our fixed child order quantity
        Assertions.assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // assert child order on the market has the expected quantity

        long expectedBidPrice = container.getState().getBidAt(0).price; // the price we expect to place the BUY order on the market with
        System.out.println("price is: " + expectedBidPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedBidPrice)); // assert child order on the market has the expected price

        /* 3. Assert no more orders are created if the trend changes e.g., if it's now more favourable to SELL now
         * This tests we don't pass the max orders that can be created in our Algorithm */
        send(createTickSELLHigh());
        send(createTickBUYLow());
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        /* 3. Check orders are filled when the right opportunity presents itself */
        send(createTickFillBUYOrders());
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // check market is closed
        send(createTickBUYLow());
        /* Assert there are still 3 active orders after market closes */
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());
    }

    @Test
    public void testSellAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        Assertions.assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to SELL HIGH, 3 SELL orders are created */
        container.setLogic(marketIsForcedOpenInstance);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickSELLHigh());
        send(createTickSELLHigh());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        Assertions.assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));

        long expectedChildOrderQuantity = 100; // our fixed child order quantity
        Assertions.assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // assert child order on the market has the expected quantity

        long expectedAskPrice = container.getState().getAskAt(0).price; // the price we expect to place the SELL order on the market with
        System.out.println("price is: " + expectedAskPrice);
        Assertions.assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedAskPrice)); // assert child order on the market has the expected price

        /* 3. Assert no more orders are created if the trend changes e.g., if it's now more favourable to BUY now
         * This tests we don't pass the max orders that can be created in our Algorithm */
        send(createTickSELLHigh());
        send(createTickBUYLow());
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        /* 4. Check orders are filled when the right opportunity presents itself */
        send(createTickFillSELLOrders());
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
//        assertEquals(300, filledQuantity); // not filling SELL orders now to test cancellation of orders on the market

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // check market is closed
        send(createTickSELLHigh());
        /* Assert there are still 3 total orders but 0 active as these orders have been cancelled because they are not filled by the time the market closes */
        assertEquals(0, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());


        // 6. Profit made after BUYING at 96 as per testBUYCondition, the profit is:
        System.out.println("Profit made is: " + (expectedAskPrice - 96));
    }

    // Test 1: Check isMarketClosed behaves as expected
    @Test
    public void testIsMarketClosedMethod(){
        // Check the function returns true when the market is closed in real time and false when it's open in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 0);
        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());
        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertEquals(isMarketClosedTestVariable, logicInstance.isMarketClosed());

        // Also check that the market instances for OPEN or CLOSED used throughout tests are working as expected
        assertFalse(marketIsForcedOpenInstance.isMarketClosed()); // returns false for forced open instance
        Assertions.assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // returns true for forced closed instance
    }

    @Test
    public void testMovingWeightAverageCalculatorMethod(){
        List<OrderBookService.OrderBookLevel> orderArray = new ArrayList<>();
        OrderBookService.OrderBookLevel order1 = new OrderBookService.OrderBookLevel(100, 100);
        OrderBookService.OrderBookLevel order2 = new OrderBookService.OrderBookLevel(90, 200);
        OrderBookService.OrderBookLevel order3 = new OrderBookService.OrderBookLevel(80, 300);
        orderArray.addAll(Arrays.asList(order1, order2 ,order3));
        double movingWeightAverage = mwaCalculator.calculateMovingWeightAverage(orderArray);
        assertEquals(86.67, movingWeightAverage, 0.01);
    }

    @Test
    public void testTrendEvaluatorMethod(){
        List<Double> listOfAverages = Arrays.asList(90.0, 91.0, 92.0, 93.0, 94.0, 95.0);
        assertEquals(5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);

        List<Double> listOfAverages2 = Arrays.asList(95.0, 94.0, 93.0, 92.0, 91.0, 90.0);
        assertEquals(-5, logicInstance.evaluateTrendUsingMWAList(listOfAverages2), 0.1);
    }

    @Test
    public void testNoActionReturnedWithInsufficientAverages() throws Exception {
        container.setLogic(marketIsForcedOpenInstance);
        send(createTick0()); // 1st average
        send(createTick0()); // 2nd average
        send(createTick0()); // 3rd average
        send(createTick0()); // 4th average

        /* Assert we return No action & no orders are not created because we only have 4 averages and need 6 for the overall trend */
        Assertions.assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = marketIsForcedOpenInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

    @Test
    public void testStableMarketAction() throws  Exception{
        /* Test that if the market is forced Open and enough data is collected on market trends where trend is stable (no overall or minimal change) - zero orders are created */
        container.setLogic(marketIsForcedOpenInstance);
        assertFalse(marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        Assertions.assertTrue(container.getState().getActiveChildOrders().isEmpty());
        Assertions.assertTrue(container.getState().getChildOrders().isEmpty());

        /* Test triggering the market again doesn't create new orders again*/
        send(createTick0());
        send(createTick0());
        Assertions.assertTrue(container.getState().getChildOrders().isEmpty());
        Assertions.assertTrue(container.getState().getActiveChildOrders().isEmpty());
    }

    // Test 1: Check isMarketClosed behaves as expected
    @Test
    public void testIsMarketClosedFunction() {
        // Check the function returns true when the market is closed in real time and false when it's open in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 0);
        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());
        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertEquals(isMarketClosedTestVariable, logicInstance.isMarketClosed());

        // Also check that the market instances for OPEN or CLOSED used throughout tests are working as expected
        assertFalse(marketIsForcedOpenInstance.isMarketClosed()); // returns false for forced open instance
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // returns true for forced closed instance
    }

    @Test
    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // check market if forced closed

        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

}