package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StretchAlgoBackTest extends AbstractAlgoBackTest {
    private MarketStatus marketStatus;
    private StretchAlgoLogic logicInstance;
    @Override
    public AlgoLogic createAlgoLogic() {
        marketStatus = mock(MarketStatus.class);
        logicInstance = new StretchAlgoLogic(marketStatus, new OrderBookService(), new MovingWeightAverageCalculator());
        return logicInstance;
    }


    @Test
    public void testBuyAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created */
        when(marketStatus.isMarketClosed()).thenReturn(false);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickBUYLow());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));

        long expectedChildOrderQuantity = 100; // our fixed child order quantity

        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // assert child order on the market has the expected quantity

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
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        when(marketStatus.isMarketClosed()).thenReturn(true);
        assertTrue(logicInstance.isMarketClosed()); // check market is closed
        send(createTickBUYLow());
        /* Assert there are still 3 active orders after market closes */
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());
    }

    @Test
    public void testSellAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to SELL HIGH, 3 SELL orders are created */
        when(marketStatus.isMarketClosed()).thenReturn(false);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickSELLHigh());
        send(createTickSELLHigh());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));

        long expectedChildOrderQuantity = 100; // our fixed child order quantity
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // assert child order on the market has the expected quantity

        long expectedAskPrice = container.getState().getAskAt(0).price; // the price we expect to place the SELL order on the market with
        System.out.println("price is: " + expectedAskPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedAskPrice)); // assert child order on the market has the expected price

        /* 3. Assert no more orders are created if the trend changes e.g., if it's now more favourable to BUY now
         * This tests we don't pass the max orders that can be created in our Algorithm */
        send(createTickSELLHigh());
        send(createTickBUYLow());
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        /* 4. Check orders are filled when the right opportunity presents itself */
        send(createTickFillSELLOrders());
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(0, filledQuantity); // not filling SELL orders now to test cancellation of orders on the market

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        when(marketStatus.isMarketClosed()).thenReturn(true);
        assertTrue(logicInstance.isMarketClosed()); // check market is closed
        send(createTickSELLHigh());
        /* Assert there are still 3 total orders but 0 active as these orders have been cancelled because they are not filled by the time the market closes */
        assertEquals(0, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());


        // 6. Profit made after BUYING at 96 as per testBUYCondition, the profit is:
        System.out.println("Profit made is: " + (expectedAskPrice - 96));
    }

    // Check isMarketClosed behaves as expected
    @Test
    public void testIsMarketClosedMethod(){
        // Check the function returns true when the market is closed in real time and false when it's open in real time LONDON time zone
        when(marketStatus.isMarketClosed()).thenReturn(true);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 0);
        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());
        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertEquals(isMarketClosedTestVariable, logicInstance.isMarketClosed());

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
        /* Test that if the market is forced Open and insufficient data is collected on market trends - zero orders are created */
        when(marketStatus.isMarketClosed()).thenReturn(false);
        send(createTick0()); // 1st average
        send(createTick0()); // 2nd average
        send(createTick0()); // 3rd average
        send(createTick0()); // 4th average

        /* Assert we return No action & no orders are not created because we only have 4 averages and need 6 for the overall trend */
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

    @Test
    public void testStableMarketAction() throws  Exception{
        /* Test that if the market is forced Open and enough data is collected on market trends where trend is stable (no overall or minimal change) - zero orders are created */
        when(marketStatus.isMarketClosed()).thenReturn(false);
        assertFalse(logicInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* Test triggering the market again doesn't create new orders again*/
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getChildOrders().isEmpty());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
    }


    @Test
    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
        when(marketStatus.isMarketClosed()).thenReturn(true);
        assertTrue(logicInstance.isMarketClosed()); // check market if forced closed

        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

}