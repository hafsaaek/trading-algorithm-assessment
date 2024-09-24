package codingblackfemales.gettingstarted;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class StretchAlgoBackTest extends SequencerTestCase {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final BookUpdateEncoder encoder = new BookUpdateEncoder();

    private AlgoContainer container;

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
        container.setLogic(new StretchAlgoLogic());

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

    private UnsafeBuffer createSampleMarketDataTick3(){
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
                .next().price(100L).size(501L)
                .next().price(101L).size(200L)
                .next().price(110L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    @Test // This tests according to real time when the stock exchange is open/closed
    public void testExampleBackTest() throws Exception {
        StretchAlgoLogic stretchInstance = new StretchAlgoLogic();

        //create a sample market data tick....
        send(createSampleMarketDataTick());

        if (stretchInstance.isMarketClosed()) {
            assertEquals(container.getState().getChildOrders().size(), 3);  //simple assert to check we had 3 orders created if market is open
            assertEquals(container.getState().getActiveChildOrders().size(), 3);  // assert to check we had 3 active orders on the market is open when the market is not in our favour and the stock exchange is open
        } else{
            assertEquals(container.getState().getChildOrders().size(), 0);
        }

        //when: market data moves towards us
        send(createSampleMarketDataTick2());

        if (stretchInstance.isMarketClosed()) {
            long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
            assertEquals(225, filledQuantity); // assert our orders have been filled if market is still open when second tick is sent
        } else {
            assertTrue(container.getState().getActiveChildOrders().isEmpty()); // assert that active orders have been cancelled if market closes after sending second tick
        }
    }

    @Test
    public void testOrdersAreNotCancelledWhenMarketIsForcedOpen() throws Exception {
        // force market to be open by using an instance of the algo class and override the
        StretchAlgoLogic stretchInstanceForceMarketOpen = new StretchAlgoLogic() {
            @Override
            public boolean isMarketClosed() {
                return false;
            }
        };

        container.setLogic(stretchInstanceForceMarketOpen);

        // Simulate market data
        send(createSampleMarketDataTick());
        //simple assert to check we had 3 orders created
        assertEquals(container.getState().getChildOrders().size(), 3);

        //when: market data moves towards us
        send(createSampleMarketDataTick2());

        //then: get the state
        var state = container.getState();
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        long parentOrder = 300;
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(parentOrder, filledQuantity);
        // assert to ensure that when the market is not closed - orders have not been cancelled - filled orders must remain active
        assertEquals(state.getActiveChildOrders().size(), 3);
    }

    @Test
    public void testCancellingOrdersAfterMarketCloses() throws Exception{
        //create a sample market data tick....
        send(createSampleMarketDataTick());

        //when: market data is not in our favour
        send(createSampleMarketDataTick3());

        // assert tha the orders have been cancelled as local time is past 4.30pm - Day order decision fu-filled
        assertEquals(container.getState().getActiveChildOrders().size(), 0);

    }
}

