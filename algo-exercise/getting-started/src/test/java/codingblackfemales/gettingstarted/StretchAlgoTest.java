//package codingblackfemales.gettingstarted;
//
//import codingblackfemales.action.Action;
//import codingblackfemales.action.NoAction;
//import codingblackfemales.container.Actioner;
//import codingblackfemales.container.AlgoContainer;
//import codingblackfemales.container.RunTrigger;
//import codingblackfemales.sequencer.DefaultSequencer;
//import codingblackfemales.sequencer.Sequencer;
//import codingblackfemales.sequencer.consumer.LoggingConsumer;
//import codingblackfemales.sequencer.marketdata.SequencerTestCase;
//import codingblackfemales.sequencer.net.TestNetwork;
//import codingblackfemales.service.MarketDataService;
//import codingblackfemales.service.OrderService;
//import codingblackfemales.sotw.ChildOrder;
//import messages.marketdata.*;
//import org.agrona.concurrent.UnsafeBuffer;
//import org.junit.jupiter.api.Test;
//
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//
//public  class StretchAlgoTest extends SequencerTestCase {
//
//
//    protected AlgoContainer container;
//
//    @Override
//    public Sequencer getSequencer() {
//        final TestNetwork network = new TestNetwork();
//        final Sequencer sequencer = new DefaultSequencer(network);
//
//        final RunTrigger runTrigger = new RunTrigger();
//        final Actioner actioner = new Actioner(sequencer);
//
//        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
//        //set my algo logic
//        container.setLogic(new StretchAlgoLogic());
//
//        network.addConsumer(new LoggingConsumer());
//        network.addConsumer(container.getMarketDataService());
//        network.addConsumer(container.getOrderService());
//        network.addConsumer(container);
//
//        return sequencer;
//    }
//
//
//    protected UnsafeBuffer createTick(){
//        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        final BookUpdateEncoder encoder = new BookUpdateEncoder();
//        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
//        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//        //write the encoded output to the direct buffer
//        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//        //set the fields to desired values
//        encoder.venue(Venue.XLON);
//        encoder.instrumentId(123L);
//        encoder.bidBookCount(3)
//                .next().price(98L).size(100L)
//                .next().price(95L).size(200L)
//                .next().price(91L).size(300L);
//
//        encoder.askBookCount(4)
//                .next().price(100L).size(101L)
//                .next().price(110L).size(200L)
//                .next().price(115L).size(5000L)
//                .next().price(119L).size(5600L);
//
//        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
//        encoder.source(Source.STREAM);
//        return directBuffer;
//    }
//
//    protected UnsafeBuffer createTickLargeSpread() {
//        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        final BookUpdateEncoder encoder = new BookUpdateEncoder();
//
//
//        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
//        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//
//        //write the encoded output to the direct buffer
//        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//
//        //set the fields to desired values
//        encoder.venue(Venue.XLON);
//        encoder.instrumentId(123L);
//        encoder.source(Source.STREAM);
//
//        encoder.bidBookCount(6)
//                .next().price(95L).size(100L)
//                .next().price(94L).size(100L)
//                .next().price(93L).size(200L)
//                .next().price(93L).size(200L)
//                .next().price(92L).size(200L)
//                .next().price(91L).size(300L);
//
//        encoder.askBookCount(6)
//                .next().price(98L).size(100L)
//                .next().price(99L).size(100L)
//                .next().price(100L).size(200L)
//                .next().price(101L).size(200L)
//                .next().price(110L).size(5000L)
//                .next().price(119L).size(5600L);
//
//        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
//
//        return directBuffer;
//    }
//    protected UnsafeBuffer createTickStableMarket(){
//        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        final BookUpdateEncoder encoder = new BookUpdateEncoder();
//        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
//        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//        //write the encoded output to the direct buffer
//        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//        //set the fields to desired values
//        encoder.venue(Venue.XLON);
//        encoder.instrumentId(123L);
//        encoder.source(Source.STREAM);
//
//        encoder.bidBookCount(6)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L);
//        encoder.askBookCount(6)
//                .next().price(96L).size(100L)
//                .next().price(96L).size(100L)
//                .next().price(96L).size(100L)
//                .next().price(96L).size(100L)
//                .next().price(96L).size(100L)
//                .next().price(96L).size(100L);
//        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
//
//        return directBuffer;
//    }
//
//    protected UnsafeBuffer createTickAskMarketFavourable(){
//        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        final BookUpdateEncoder encoder = new BookUpdateEncoder();
//
//
//        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
//        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//
//        //write the encoded output to the direct buffer
//        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//
//        //set the fields to desired values
//        encoder.venue(Venue.XLON);
//        encoder.instrumentId(123L);
//        encoder.source(Source.STREAM);
//
//        encoder.bidBookCount(6)
//                .next().price(96L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(100L);
//
//        encoder.askBookCount(6)
//                .next().price(98L).size(100L)
//                .next().price(99L).size(100L)
//                .next().price(100L).size(200L)
//                .next().price(101L).size(200L)
//                .next().price(110L).size(5000L)
//                .next().price(119L).size(5600L);
//
//        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
//
//        return directBuffer;
//    }
//    protected UnsafeBuffer createTickBidMarketFavourable(){
//        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        final BookUpdateEncoder encoder = new BookUpdateEncoder();
//        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
//        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//        //write the encoded output to the direct buffer
//        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//        //set the fields to desired values
//        encoder.venue(Venue.XLON);
//        encoder.instrumentId(123L);
//        encoder.source(Source.STREAM);
//
//        encoder.bidBookCount(6)
//                .next().price(95L).size(100L)
//                .next().price(95L).size(150L)
//                .next().price(94L).size(100L)
//                .next().price(94L).size(150L)
//                .next().price(93L).size(200L)
//                .next().price(92L).size(300L);
//        encoder.askBookCount(6)
//                .next().price(98L).size(100L)
//                .next().price(97L).size(100L)
//                .next().price(96L).size(100L)
//                .next().price(95L).size(100L)
//                .next().price(93L).size(100L)
//                .next().price(94L).size(100L);
//        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
//
//        return directBuffer;
//    }
//
//    StretchAlgoLogic stretchLogic = new StretchAlgoLogic();
//
//    @Test
//    public void testDispatchThroughSequencer() throws Exception {
//
//        //create a sample market data tick....
//        send(createTick());
//        //simple assert to check we don't create orders as we only have 3 bids and 3 asks and need 6 & 6 respectively
//        assertEquals(0, container.getState().getChildOrders().size());
//
//        // Tick 2 has enough data to evaluate market trend but spread is too large to place orders - assert no orders have been created
//        send(createTickLargeSpread());
//        assertEquals(0, container.getState().getChildOrders().size());
//        assertEquals(0, container.getState().getActiveChildOrders().size());
//
//        // Tick 3 has enough data to evaluate market trend but the order-book trend is overall stable, therefore we should return No action here   - assert
//        send(createTickStableMarket());
//        double TREND_THRESHOLD = 0.5;
//        Action returnAction = stretchLogic.evaluate(container.getState());
//        assertEquals(NoAction.class, returnAction.getClass());
//        assertEquals(0, container.getState().getChildOrders().size());
//        // creating asserts to test we calculate the right MWA trend which should be zero as prices on both sides have not changed at all
//        HashMap<String, List<StretchAlgoLogic.OrderBookLevel>> orderLevelsMap = stretchLogic.getOrderBookLevels(container.getState());
//        double bidMarketTrend = stretchLogic.evaluateTrendUsingMWA("Bid", orderLevelsMap.get("Bid"));
//        double askMarketTrend = stretchLogic.evaluateTrendUsingMWA("Ask", orderLevelsMap.get("Ask"));
//        assertTrue(bidMarketTrend == 0 && bidMarketTrend < TREND_THRESHOLD);
//        assertTrue(askMarketTrend == 0 && askMarketTrend < TREND_THRESHOLD);
//    }
//
//    @Test
//    public void testAskOrdersCreatedWhenAskSideTrendExceedsThreshold() throws Exception {
//        assertTrue(container.getState().getActiveChildOrders().size() == 0);
//
//        send(createTickAskMarketFavourable());
//
//        assertTrue(container.getState().getActiveChildOrders().size() == 3);
//        assertTrue(container.getState().getActiveChildOrders().get(0).getSide().toString() == "SELL");
//        assertTrue(container.getState().getActiveChildOrders().get(1).getSide().toString() == "SELL");
//        assertTrue(container.getState().getActiveChildOrders().get(2).getSide().toString() == "SELL");
//
//        send(createTickBidMarketFavourable());
//        assertTrue(container.getState().getActiveChildOrders().size() == 3);
//
//        send(createTickAskMarketFavourable());
//        assertTrue(container.getState().getChildOrders().size() <= 3);
//
//    }
//
//    @Test
//    public void testBidOrdersCreatedWhenBidSideTrendExceedsThreshold() throws Exception {
//        assertEquals(0, container.getState().getChildOrders().size());
//
//        send(createTickBidMarketFavourable());
//        assertTrue(container.getState().getActiveChildOrders().size() == 3);
//        assertTrue(container.getState().getActiveChildOrders().get(0).getSide().toString() == "BUY");
//        assertTrue(container.getState().getActiveChildOrders().get(1).getSide().toString().equals("BUY"));
//        assertTrue(container.getState().getActiveChildOrders().get(2).getSide().toString() == "BUY");
//
//        // Assert no more
//        send(createTickBidMarketFavourable());
//        assertEquals(3, container.getState().getChildOrders().size());
//    }
//
////    @Test // - Use in back-test
////    public void testOverExecution() throws Exception {
////        // Sending first tick should trigger no matches
////        send(createTick()); // Creates the initial conditions with no fills
////
////        // To test that if more than 3 orders (max fill-able child orders) is possible
////        send(createTickBidMarketFavourable());
////        long parentOrderQuantity = 300;
////        var state = container.getState();
////        long filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
////
////        long filledOrdersCount = state.getActiveChildOrders().stream()
////                .filter(order -> order.getFilledQuantity() == 100).count();
////
////        // Check the final state
////        assertEquals(3, filledOrdersCount); // Three orders should be filled
////        assertEquals(parentOrderQuantity, filledQuantity); // Only 300 shares (parent order) should be bought assertion
////
////        // re-trigger market data to ensure no more orders are created and over-execution is avoided
////        send(createTickBidMarketFavourable());
////        state = container.getState();
////        filledQuantity = state.getActiveChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();
////        assertTrue(filledQuantity != 400);
////        assertTrue(filledQuantity == 300);
////
////    }
//
//    // Test your logic is making money by comparing what you have filled - for Back tests
//
//// Tests: testing everything except for orders being filled
//    // 1. No action returned? When would this happen?
//        // 1. Less than 6 levels available on the market on each side - assert no orders are created/on the market as we can't calculate MWA yet DONE
//        // 2. Less than 6 levels available on the market on each side - a way to assert we return 0.0 as the MWA on both sides? DONE (MWA test covered in 4)
//        // 3. Spread is too high - zero orders should be on the market DONE
//        // 4. Stable market - return no action and no children created and MWA is 0 and less than threshold DONE
//        // 4. Max orders created - assert only 3 orders on the market (active) and no more than 3 DONE
//        // 6. Parent order/3 orders have been filled - assert no more orders created and NO action class is returned TO TEST IN BACK-TEST WHERE ORDERS ARE FILL-ABLE
//
//    // Main tests: order creation, max limit, stable market, bid favourable market, ask favourable market, high spread, enough data for MWA calculation, MWA
//
//
//
//}
