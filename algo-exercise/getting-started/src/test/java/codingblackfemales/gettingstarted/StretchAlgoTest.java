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
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;


public  class StretchAlgoTest extends SequencerTestCase {


    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        container.setLogic(new StretchAlgoLogic());

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(container);

        return sequencer;
    }


    protected UnsafeBuffer createTick0(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(3)
                .next().price(98L).size(100L)
                .next().price(95L).size(200L)
                .next().price(94L).size(300L);;

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

    private StretchAlgoLogic logicInstance;
    private StretchAlgoLogic marketIsForcedOpenInstance;
    private StretchAlgoLogic marketIsForcedClosedInstance;

    @BeforeEach
    public void setUp(){
        logicInstance = new StretchAlgoLogic();
        marketIsForcedOpenInstance = new StretchAlgoLogic() {
            @Override
            public boolean  isMarketClosed() {
                return false;
            }
        };

        marketIsForcedClosedInstance = new StretchAlgoLogic() {
            @Override
            public boolean  isMarketClosed() {
                return true;
            }
        };
    }

    // Test 1: Check isMarketClosed behaves as expected
    @Test
    public void testIsMarketClosedFunction() throws Exception{
        // Check the function returns true when the market is closed in real time and false when it's open in real time LONDON time zone
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 00);
        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketOpenTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());
        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertTrue(isMarketClosedTestVariable == logicInstance.isMarketClosed());

        // Also check that the market instances for OPEN or CLOSED used throughout tests are working as expected
        assertFalse(marketIsForcedOpenInstance.isMarketClosed()); // returns false for forced open instance
        assertTrue(marketIsForcedClosedInstance.isMarketClosed()); // returns true for forced closed instance
    }

    // Test 2: Now that testIsMarketClosedFunction is working as expected - check other areas of the code
        // a. Test no orders are created if market is closed
        // b. Test no orders are created till we have 6 MWAs
        // c. Test no orders are created if trend is stable
        // d. send more ticks to trigger creating an order - 3 should be created
        // e. send another tick to confirm no more than 3 have been created
        // f. check orders are cancelled if market is force closed

    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // 1. check no orders are on the market before triggering logic container
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 2. check no orders are created if the market is closed
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        send(createTick0());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass()); // assert 'No action' action is returned

        // 3. Test that if the market is forced Open and enough data is collected on market trends where trend is stable (no overall or minimal change) - zero orders are created
        container.setLogic(marketIsForcedOpenInstance);
        assertTrue(!marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());

        // 4. Test that if the market is forced Open and enough data is collected on market trends to BUY LOW, 3 BUY orders are created
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));
        long expectedChildOrderQuantity = 100;
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // asser quantity is
        long expectedBidPrice = container.getState().getBidAt(0).price;
        System.out.println("price is: " + expectedBidPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedBidPrice)); //


        // 5. Assert that the average calculator methods works as expected for order books (basic tick and BUY tick)
        // ideally would want to calculate the order book of 5 iterations of createTick() and 1 of createTickBidMarketFavourable()

        // 6. Assert that the average trend evaluation using list of averages is working as expected
        List<Double> listOfAverages = Arrays.asList(90.0, 91.0, 92.0, 93.0, 94.0, 95.0);
        assertEquals(5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);

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
        assertTrue(container.getState().getChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());

    }

    @Test
    public void testSellCondition() throws Exception {
        // 1. check no orders are on the market before triggering logic container
        assertTrue(container.getState().getChildOrders().isEmpty());

        // 2. check no orders are created if the market is closed
        container.setLogic(marketIsForcedClosedInstance); // should return no action and say we can't place orders because the market is closed
        send(createTick0());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        Action returnAction = logicInstance.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass()); // assert 'No action' action is returned

        // 3. Test that if the market is forced Open and enough data is collected on market trends to SELL HIGH , 3 SELL orders are created
        container.setLogic(marketIsForcedOpenInstance);
        assertTrue(!marketIsForcedOpenInstance.isMarketClosed());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTick0());
        send(createTickSELLHigh());
        send(createTickSELLHigh());
        assertTrue(container.getState().getActiveChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
        long expectedChildOrderQuantity = 100;
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == expectedChildOrderQuantity)); // asser quantity is
        long expectedBidPrice = container.getState().getAskAt(0).price;
        System.out.println("Placed 3 ask orders with ask price: " + expectedBidPrice);
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getPrice() == expectedBidPrice)); //


        // 5. Assert that the average calculator methods works as expected for order books (basic tick and BUY tick)
        // ideally would want to calculate the order book of 5 iterations of createTick() and 1 of createTickBidMarketFavourable()

        // 6. Assert that the average trend evaluation using list of averages is working as expected
        List<Double> listOfAverages = Arrays.asList(95.0, 94.0, 93.0, 92.0, 91.0, 90.0);
        assertEquals(-5, logicInstance.evaluateTrendUsingMWAList(listOfAverages), 0.1);

        // 7. Assert no more orders are created if the trend changes e.g., it's more favourable to SELL now (we don't have stocks to sell yet, should still BUY)
        send(createTickBUYLow());
        send(createTickBUYLow());
        send(createTickSELLHigh());
        assertTrue(container.getState().getChildOrders().size() == 3);
        assertTrue(container.getState().getActiveChildOrders().size() == 3);

        // 8. test these ALL ACTIVE orders are cancelled if the market closes
        container.setLogic(marketIsForcedClosedInstance);
        send(createTickBUYLow());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
        assertTrue(container.getState().getChildOrders().size() == 3); // to assert no active orders but there are 3 CANCELLED orders
    }

}
