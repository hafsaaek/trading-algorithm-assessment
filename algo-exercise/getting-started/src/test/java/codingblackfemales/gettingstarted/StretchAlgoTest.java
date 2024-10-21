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
import org.junit.Assert;
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


public  class StretchAlgoTest extends SequencerTestCase {
    private MarketStatus marketStatus;
    private StretchAlgoLogic logicInstance;

    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        marketStatus = mock(MarketStatus.class);
        logicInstance = new StretchAlgoLogic(marketStatus, new OrderBookService(), new MovingWeightAverageCalculator());
        container.setLogic(logicInstance);

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


    @Test
    public void testIsMarketOpenMethod() {
        // Check the function returns true when the market is open in real time and false when it's closed in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 0);

        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketCloseTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketOpen());

        // if boolean : true --> isMarketClosed() should also return true and vice versa
        Assert.assertEquals(isMarketClosedTestVariable, logicInstance.isMarketOpen());
    }

    @Test
    public void testNoOrdersCreatedIfMarketClosed() throws Exception {
        when(marketStatus.isMarketOpen()).thenReturn(false);
        send(createTick0());
        send(createTickBUYLow());
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }


    @Test
    public void testTrendEvaluatorMethod(){
        List<Double> listOfAverages = Arrays.asList(90.0, 91.0, 92.0, 93.0, 94.0, 95.0);
        assertEquals(5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);

        List<Double> listOfAverages2 = Arrays.asList(95.5, 95.0, 94.5, 94.0, 93.5, 93.0);
        assertEquals(-2.5, logicInstance.evaluateTrendUsingMWAList(listOfAverages2), 0.1);

        List<Double> listOfAveragesEmpty = new ArrayList<>() ; // no values - should return 0
        assertEquals(0, logicInstance.evaluateTrendUsingMWAList(listOfAveragesEmpty), 0.1);
    }

    @Test
    public void testNoActionReturnedWithInsufficientAverages() throws Exception {
        /* Test that if the market is forced Open and insufficient data is collected on market trends - zero orders are created */
        when(marketStatus.isMarketOpen()).thenReturn(true);
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
        when(marketStatus.isMarketOpen()).thenReturn(true);
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getChildOrders().isEmpty()); // assert no orders created as there has been no overall change in market trends
    }


    @Test
    public void testBuyAction() throws Exception {
        /* 1. check no orders are on the market before triggering logic container */
        assertTrue(container.getState().getChildOrders().isEmpty());

        /* 2. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created */
        when(marketStatus.isMarketOpen()).thenReturn(true);
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
        when(marketStatus.isMarketOpen()).thenReturn(false);
        assertFalse(logicInstance.isMarketOpen()); // check market is closed
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
        when(marketStatus.isMarketOpen()).thenReturn(true);
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

        // 4. test these ALL ACTIVE orders are cancelled if not filled by end of day
        when(marketStatus.isMarketOpen()).thenReturn(false);
        assertFalse(logicInstance.isMarketOpen()); // check market is closed
        send(createTickSELLHigh());
        /* Assert there are no active orders as we've cancelled them but there are 3 total orders to account for those cancelled [IF IT HAS NOT FILLED] */
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertEquals(3, container.getState().getChildOrders().size());

        // send another tick to check the program returns no action
        send(createTickSELLHigh());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());
    }

}