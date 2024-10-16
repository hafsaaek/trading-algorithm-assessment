package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public  class StretchAlgoTest extends SequencerTestCase {
    private MarketStatus marketStatus;
    private MovingWeightAverageCalculator mwaCalculator;
    private OrderBookService orderBookService;
    private StretchAlgoLogic logicInstance;
    private StretchAlgoLogic marketIsForcedOpenInstance;
    private StretchAlgoLogic marketIsForcedClosedInstance;

    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        container.setLogic(new StretchAlgoLogic(marketStatus, orderBookService, mwaCalculator));

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(container);

        return sequencer;
    }

    protected UnsafeBuffer createTick0(){ // UnsafeBuffer provides low-level access to memory, allowing for faster operations
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder(); // header of message is encoded to binary format
        final BookUpdateEncoder encoder = new BookUpdateEncoder(); // contents of messages (OB) is also encoded for computer processing
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer); // store content in a buffer with 1024 bytes of data
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);   //write the encoded output to the direct buffer and spit out the header
        //set the fields to desired values
        encoder.venue(Venue.XLON); // specified we are trading stock in LSEG
        encoder.instrumentId(123L); // not really sure
        encoder.bidBookCount(3)
                .next().price(98L).size(100L)
                .next().price(95L).size(200L)
                .next().price(94L).size(300L);

        encoder.askBookCount(3)
                .next().price(100L).size(101L)
                .next().price(105L).size(200L)
                .next().price(110L).size(300L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    protected UnsafeBuffer createTickSELLHigh(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();


        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(100L).size(100L)
                .next().price(102L).size(200L)
                .next().price(103L).size(300L);

        encoder.askBookCount(3)
                .next().price(110L).size(100L)
                .next().price(115L).size(200L)
                .next().price(119L).size(300L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }
    protected UnsafeBuffer createTickBUYLow(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(96L).size(100L)
                .next().price(94L).size(200L)
                .next().price(94L).size(300L);
        encoder.askBookCount(3)
                .next().price(98L).size(100L)
                .next().price(99L).size(200L)
                .next().price(100L).size(300L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    @BeforeEach
    public void setUp(){
        orderBookService = new OrderBookService();
        marketStatus = new MarketStatus();
        mwaCalculator = new MovingWeightAverageCalculator(orderBookService);
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
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // returns true for forced closed instance
    }

    @Test
    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        assertTrue(marketIsForcedClosedInstance.isMarketClosed());

        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = marketIsForcedClosedInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
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
        assertTrue(container.getState().getChildOrders().isEmpty());
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
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* Test triggering the market again doesn't create new orders again*/
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getChildOrders().isEmpty());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
    }


    @Test
    public void testBuyAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created */
        container.setLogic(marketIsForcedOpenInstance);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickBUYLow());
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

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // check market is closed
        send(createTickBUYLow());
        /* Assert there are no active orders as we've cancelled them but there are 3 total orders to account for those cancelled [IF IT HAS NOT FILLED] */
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertEquals(3, container.getState().getChildOrders().size());
    }

    @Test
    public void testSellAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to SELL HIGH, 3 SELL orders are created */
        container.setLogic(marketIsForcedOpenInstance);
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
        send(createTickBUYLow());
        send(createTickSELLHigh());
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        // 4. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // check market is closed
        send(createTickSELLHigh());
        /* Assert there are no active orders as we've cancelled them but there are 3 total orders to account for those cancelled [IF IT HAS NOT FILLED] */
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertEquals(3, container.getState().getChildOrders().size());
    }


}