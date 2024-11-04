package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StretchAlgoBackTest extends AbstractAlgoBackTest {
//    private MarketStatusByMIC marketStatusByMIC;
    private MarketStatus marketStatus;
    private MarketStatusRegistry marketStatusRegistry;
    private StretchAlgoLogic logicInstance;
    LocalDate today = LocalDate.now();
    ZonedDateTime currentTime = ZonedDateTime.now();
    String marketIdentifierCode = "XLON";

    @Override
    public AlgoLogic createAlgoLogic() {
        marketStatus = mock(MarketStatus.class);
        marketStatusRegistry = mock(MarketStatusRegistry.class);
//        MarketStatusByMIC marketStatusByMIC = new SimpleMarketStatusByMIC();
        ZonedDateTime currentTime = ZonedDateTime.now();
        String marketIdentifierCode = "XLON";

        logicInstance = new StretchAlgoLogic(marketStatusRegistry, new OrderBookService(), new MovingWeightAverageCalculator(), currentTime,  marketIdentifierCode);
        return logicInstance;
    }


    @Test
    public void testBuyAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created */
        ZonedDateTime timeNow = ZonedDateTime.of(today, LocalTime.of(8, 59, 0), ZoneId.of("Europe/London"));

        when(marketStatusRegistry.getByMIC("XLON")).thenReturn(marketStatus);
        when(marketStatus.getMarketPhase(any())).thenReturn(MARKET_PHASES.OPEN); // will return x given ANY input
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

        /* 4. Check orders are filled when the right opportunity presents itself */
        send(createTickFillBUYOrders());
        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

        // 5. test these ALL ACTIVE orders are cancelled if the market closes
        currentTime = ZonedDateTime.of(today, LocalTime.of(18, 59, 0), ZoneId.of("Europe/London"));
        when(marketStatusRegistry.getByMIC("XLON")).thenReturn(marketStatus);
        when(marketStatus.getMarketPhase(any())).thenReturn(MARKET_PHASES.CLOSED);
//        assertEquals(MARKET_PHASES.CLOSED, logicInstance.getMarketPhase()); // check market is closed
        send(createTickBUYLow());
        /* Assert there are still 3 active orders after market closes */
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());
    }
//
//    @Test
//    public void testSellAction() throws Exception {
//        /* 1. check no orders are on the market before triggering logic container */
//        assertTrue(container.getState().getChildOrders().isEmpty());
//
//        /* 2. Test that if the market is forced Open and enough data is collected on market trends to SELL HIGH, 3 SELL orders are created */
//        when(marketStatus.isMarketOpen()).thenReturn(true);
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        send(createTickSELLHigh());
//        send(createTickSELLHigh());
//        assertEquals(3, container.getState().getActiveChildOrders().size());
//        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
//
//        long expectedChildOrderQuantity = 100; // our fixed child order quantity
//        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // assert child order on the market has the expected quantity
//
//        long expectedAskPrice = container.getState().getAskAt(0).price; // the price we expect to place the SELL order on the market with
//        System.out.println("price is: " + expectedAskPrice);
//        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedAskPrice)); // assert child order on the market has the expected price
//
//        /* 3. Assert no more orders are created if the trend changes e.g., if it's now more favourable to BUY now
//         * This tests we don't pass the max orders that can be created in our Algorithm */
//        send(createTickSELLHigh());
//        send(createTickBUYLow());
//        assertEquals(3, container.getState().getChildOrders().size());
//        assertEquals(3, container.getState().getActiveChildOrders().size());
//
//        /* 4. Check orders are filled when the right opportunity presents itself */
//        send(createTickFillSELLOrders());
//        //Check things like filled quantity, cancelled order count etc....
//        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
//        //and: check that our algo state was updated to reflect our fills when the market data
//        assertEquals(0, filledQuantity); // not filling SELL orders now to test cancellation of orders on the market
//
//        // 5. test these ALL ACTIVE orders are cancelled if the market closes as they have not been filled by end of day
//        when(marketStatus.isMarketOpen()).thenReturn(false);
//        assertFalse(logicInstance.isMarketOpen()); // check market is closed
//        send(createTickSELLHigh());
//        /* Assert there are still 3 total orders but 0 active as these orders have been cancelled because they are not filled by end of days */
//        assertEquals(0, container.getState().getActiveChildOrders().size());
//        assertEquals(3, container.getState().getChildOrders().size());
//
//
//        // 6. Profit made after BUYING at 96 as per testBUYCondition, the profit is:
//        System.out.println("Profit made is: " + (expectedAskPrice - 94));
//    }
//
//
//    @Test
//    public void testTrendEvaluatorMethod(){
//        List<Double> listOfAverages = Arrays.asList(90.0, 91.0, 92.0, 93.0, 94.0, 95.0); // positive trend
//        assertEquals(5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);
//
//        List<Double> listOfAverages2 = Arrays.asList(95.5, 95.0, 94.5, 94.0, 93.5, 93.0); // negative trend
//        assertEquals(-2.5, logicInstance.evaluateTrendUsingMWAList(listOfAverages2), 0.1);
//
//        List<Double> listOfAveragesEmpty = new ArrayList<>() ; // no values - should return 0
//        assertEquals(0, logicInstance.evaluateTrendUsingMWAList(listOfAveragesEmpty), 0.1);
//
//    }
//
//    @Test
//    public void testNoActionReturnedWithInsufficientAverages() throws Exception {
//        /* Test that if the market is forced Open and insufficient data is collected on market trends - zero orders are created */
//        when(marketStatus.isMarketOpen()).thenReturn(true);
//        send(createTick0()); // 1st average
//        send(createTick0()); // 2nd average
//        send(createTick0()); // 3rd average
//        send(createTick0()); // 4th average
//
//        /* Assert we return No action & no orders are not created because we only have 4 averages and need 6 for the overall trend */
//        assertTrue(container.getState().getChildOrders().isEmpty());
//        Action returnAction = logicInstance.evaluate(container.getState());
//        assertEquals(NoAction.class, returnAction.getClass());
//    }
//
//    @Test
//    public void testStableMarketAction() throws  Exception{
//        /* Test that if the market is forced Open and enough data is collected on market trends where trend is stable (no overall or minimal change) - zero orders are created */
//        when(marketStatus.isMarketOpen()).thenReturn(true);
//        assertTrue(logicInstance.isMarketOpen());
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        send(createTick0());
//        assertTrue(container.getState().getChildOrders().isEmpty()); // assert wwe have not created new orders as trend is stable
//        assertTrue(container.getState().getActiveChildOrders().isEmpty());
//    }
//
//
//    @Test
//    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
//        when(marketStatus.isMarketOpen()).thenReturn(false);
//        assertFalse(logicInstance.isMarketOpen()); // check market if forced closed
//
//        send(createTick0()); // update order book
//        send(createTickBUYLow());
//        assertTrue(container.getState().getChildOrders().isEmpty()); // assert no orders are created
//        Action returnAction = logicInstance.evaluate(container.getState());
//        assertEquals(NoAction.class, returnAction.getClass()); // assert no action is returned
//    }

}