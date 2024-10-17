package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.orderbook.OrderBook;
import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.consumer.OrderBookInboundOrderConsumer;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import codingblackfemales.sotw.ChildOrder;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MyAlgo2BackTest extends SequencerTestCase {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final BookUpdateEncoder encoder = new BookUpdateEncoder();

    private AlgoContainer container;

    private MarketStatus marketStatus;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        final MarketDataChannel marketDataChannel = new MarketDataChannel(sequencer);
        final OrderChannel orderChannel = new OrderChannel(sequencer);
        final OrderBook book = new OrderBook(marketDataChannel, orderChannel);

        final OrderBookInboundOrderConsumer orderConsumer = new OrderBookInboundOrderConsumer(book);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        marketStatus = mock(MarketStatus.class);
        container.setLogic(new MyAlgoLogic2(marketStatus));

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        return sequencer;
    }

    private UnsafeBuffer createSampleMarketDataTick(){
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(98L).size(100L)
                .next().price(95L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(100L).size(101L)
                .next().price(110L).size(200L)
                .next().price(115L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    private UnsafeBuffer createSampleMarketDataTick2(){
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(95L).size(100L)
                .next().price(93L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(98L).size(501L)
                .next().price(101L).size(200L)
                .next().price(110L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }


    MyAlgoLogic2 algoLogic2 = new MyAlgoLogic2(marketStatus);

    /* This tests according the logic class's return types in real time relative to when the stock exchange is open/closed */
    @Test
    public void testDispatchThroughSequencer() throws Exception {
        //create a sample market data tick....
        send(createSampleMarketDataTick());
        // this will only test one condition at a time - never both at the same time
        // pass time to the test - e.g, sunday, or
        if (marketStatus.isMarketOpen()) { // if market is open --> 3 orders should be on the market
            assertEquals(3, container.getState().getChildOrders().size());  //simple assert to check we had 3 orders created if market is open
            assertEquals(3, container.getState().getActiveChildOrders().size());  // assert to check we had 3 active orders on the market is open when the market is not in our favour and the stock exchange is open
        } else{ // if market is closed --> no orders should be on the market
            assertEquals(0, container.getState().getChildOrders().size());
        }

        //when: market data moves towards us
        send(createSampleMarketDataTick2());

        if (marketStatus.isMarketOpen()) { // if market is open --> orders should be filled
            long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
            assertEquals(300, filledQuantity); // assert our orders have been filled if market is still open when second tick is sent
        } else {
            assertTrue(container.getState().getActiveChildOrders().isEmpty()); // assert that active orders have been cancelled if market closes after sending second tick
        }
    }

    @Test
    public void testFilledOrdersAreNotCancelled() throws Exception {
        // force market to be open
        when(marketStatus.isMarketOpen()).thenReturn(true);
        // Simulate market data
        send(createSampleMarketDataTick());
        //simple assert to check we had 3 orders created
        assertEquals(container.getState().getChildOrders().size(), 3);

        //when: market data moves towards us
        send(createSampleMarketDataTick2());

        //then: get the state
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).orElse(0L);
        long parentOrder = 300;
        assertEquals(parentOrder, filledQuantity); // assert all orders are filled
        // assert to ensure that when the market is closed - orders have not been cancelled - filled orders must remain active
        when(marketStatus.isMarketOpen()).thenReturn(false); // close market
        Action returnAction = algoLogic2.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());

        assertEquals(state.getActiveChildOrders().size(), 3); // check filled orders are not cancelled
    }

    @Test
    public void testNonFilledOrdersCancelledWhenMarketCloses() throws Exception{
        when(marketStatus.isMarketOpen()).thenReturn(true);
        send(createSampleMarketDataTick());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());

        /* Assert orders are cancelled if not filled by end of day */
        when(marketStatus.isMarketOpen()).thenReturn(false);
        send(createSampleMarketDataTick());
        assertEquals(0, container.getState().getActiveChildOrders().size());
        assertEquals(3, container.getState().getChildOrders().size());

        // send another tick to check the program returns no action
        send(createSampleMarketDataTick());
        Action returnAction = algoLogic2.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());

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
        System.out.println(marketStatus.isMarketOpen());

        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertEquals(isMarketClosedTestVariable, marketStatus.isMarketOpen());
    }
}

