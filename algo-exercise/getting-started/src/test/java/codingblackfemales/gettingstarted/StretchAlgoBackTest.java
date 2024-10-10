package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
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
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StretchAlgoBackTest extends SequencerTestCase {


    protected AlgoContainer container;

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
        container.setLogic(new OldStretchAlgoLogic());

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        return sequencer;
    }

//    public abstract AlgoLogic createAlgoLogic();

    protected UnsafeBuffer createTickNotEnoughLevelsForWMA(){
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
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(100L).size(101L)
                .next().price(110L).size(200L)
                .next().price(115L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    protected UnsafeBuffer createTickVolatileMarket(){
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

        // bid prices are decreasing and ask is increasing leading to a large spread and widening gap - volatile market
        // good place to not create orders
        encoder.bidBookCount(6)
                .next().price(100).size(100L)
                .next().price(99).size(100L)
                .next().price(98L).size(100L)
                .next().price(97).size(200L)
                .next().price(96).size(300L)
                .next().price(95L).size(400L);

        encoder.askBookCount(6)
                .next().price(105L).size(101L)
                .next().price(110L).size(200L)
                .next().price(112L).size(300L)
                .next().price(115L).size(500L)
                .next().price(117L).size(500L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTickStableMarket(){
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

        // stable market: bid mwa: , ask mwa: 0.17 (both are < trend threshold which is 0.5)
        encoder.bidBookCount(6)
                .next().price(95L).size(100L)
                .next().price(95L).size(100L)
                .next().price(95L).size(100L)
                .next().price(95L).size(100L)
                .next().price(95L).size(100L)
                .next().price(96L).size(100L);
        encoder.askBookCount(6)
                .next().price(96L).size(100L)
                .next().price(96L).size(100L)
                .next().price(96L).size(100L)
                .next().price(97).size(100L)
                .next().price(96L).size(100L)
                .next().price(96L).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTickBuyLow(){

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

        encoder.bidBookCount(6)
                .next().price(90L).size(100L)
                .next().price(91L).size(100L)
                .next().price(92L).size(100L)
                .next().price(93L).size(100L)
                .next().price(94L).size(100L)
                .next().price(95L).size(100L);

        encoder.askBookCount(6)
                .next().price(100L).size(100L)
                .next().price(99L).size(100L)
                .next().price(98L).size(100L)
                .next().price(97L).size(100L)
                .next().price(96L).size(100L)
//                .next().price(95).size(400L)
                .next().price(95).size(100L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    // You can use a setup method to specify which logic oyu wan to use before

    @Test
    public void testNoReturnActions() throws Exception {
        double TREND_THRESHOLD = 0.5;

        // send a sample market data that does not have enough bid Levels
        send(createTickNotEnoughLevelsForWMA());
        // simple assert to check we don't create orders as we only have 3 bids and 4 asks and need 6 & 6 respectively
        assertTrue(container.getState().getChildOrders().size() == 0);
        //logger: [STRETCH-ALGO] We do not have enough orders to evaluate the market trend, there are currently 3 bids and 4 asks

        // send a sample market data that has a large spread and volatile conditions (widening gap between bid and ask offers)
        send(createTickVolatileMarket());
        // Assert to check we have created 0 child orders as spread is >= 3 (5 for this tick)
        assertTrue(container.getState().getChildOrders().size() == 0);
        // Logger: [STRETCH-ALGO] Order book spread is currently 5 which is equal to or larger than SPREAD_THRESHOLD. Market too volatile to place an order, Returning No Action.

        // send a sample market data that stimulates a market where we would like to hold off so the trend on both sides amount the trend threshold or are slightly below it (<= 0.5)
        send(createTickStableMarket());
        assertTrue(container.getState().getChildOrders().size() == 0);
//         logger: [STRETCH-ALGO] Market is stable, holding off placing orders for the meantime. Returning No Action.
        OldStretchAlgoLogic stretchLogicBackTest = new OldStretchAlgoLogic();
        Action returnAction = stretchLogicBackTest.evaluate(container.getState());
        Assert.assertEquals(NoAction.class, returnAction.getClass());
//         creating asserts to test we calculate the right MWA trend which should be zero as prices on both sides have not changed at all
        HashMap<String, List<OldStretchAlgoLogic.OrderBookLevel>> orderLevelsMap = stretchLogicBackTest.getOrderBookLevels(container.getState()); // get map with bid and ask levels
        double bidMarketTrend = stretchLogicBackTest.evaluateTrendUsingMWA("Bid", orderLevelsMap.get("Bid")); // calculate overall bid trend
        double askMarketTrend = stretchLogicBackTest.evaluateTrendUsingMWA("Ask", orderLevelsMap.get("Ask")); // calculate overall ask trend
        assertTrue(bidMarketTrend <=TREND_THRESHOLD && askMarketTrend <= TREND_THRESHOLD); // assert the trend is very insignificant
//        assertTrue(Math.round(bidMarketTrend * 100)/100 == 0.17 && askMarketTrend == 0.17 ); // assert the trend is very insignificant - should return mwa to 2 decimal points

    }

    @Test
    public void testPlacingBUYOrdersCreatesProfit() throws Exception {
        //create a sample market data tick....
        send(createTickBuyLow());
        send(createTickBuyLow());
        send(createTickBuyLow());

        // Assert to check we have created 3 child orders under good buy conditions
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));

        //then: get the state
        var state = container.getState();

        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
//        assertEquals(300, filledQuantity);

        // A way to track profit: buy 300 shares at whatever price (look ath the first orderbook), then place a sell order for those 300 shares


    }

    // 1st test: test you can make profits when market is good to buy
    // logic: bid prices are increasing - select best bid from first tick - 3 orders are created - on side BUY - - send tick that should ensure orders are not created (stable market) -  send a tick that favours buying low - (increasing bid prices, falling ask prices) - check orders have been filled - check profit is being made (check price executed at) - profit should be price order placed on - price actually bought at - check MWA calculations - send tick that favours creating sell orders and asser no new orders are created - assert more orders are filled

    // 2nd test - same as above but for selling


}

// Test your logic is making money by comparing what you have filled - for Back tests
