package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
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

import java.nio.ByteBuffer;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {


    protected AlgoContainer container;

    @Override
    public Sequencer getSequencer() {
        final TestNetwork network = new TestNetwork();
        final Sequencer sequencer = new DefaultSequencer(network);

        final RunTrigger runTrigger = new RunTrigger();
        final Actioner actioner = new Actioner(sequencer);

        final MarketDataChannel marketDataChannel = new MarketDataChannel(sequencer);
        final OrderChannel orderChannel = new OrderChannel(sequencer);
        final OrderBook book = new OrderBook(marketDataChannel, orderChannel); // 

        final OrderBookInboundOrderConsumer orderConsumer = new OrderBookInboundOrderConsumer(book);

        container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger), runTrigger, actioner);
        //set my algo logic
        container.setLogic(createAlgoLogic());

        network.addConsumer(new LoggingConsumer());
        network.addConsumer(book);
        network.addConsumer(container.getMarketDataService());
        network.addConsumer(container.getOrderService());
        network.addConsumer(orderConsumer);
        network.addConsumer(container);

        return sequencer;
    }

    public abstract AlgoLogic createAlgoLogic();

    protected UnsafeBuffer createTick(){
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();


        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        //write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
//        18:00:14.434 [main] INFO  c.gettingstarted.StretchAlgoLogic - [STRETCH-ALGO] Market trend calculated, Bid market trend is 2.1299999999999955, Ask Market Trend is -6.609999999999999

        //set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(98L).size(98L)
                .next().price(97L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(100L).size(101L)
                .next().price(110L).size(200L)
                .next().price(115L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTick2() { // allows for 1 match
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
                .next().price(95L).size(99L)
                .next().price(93L).size(200L)
                .next().price(91L).size(300L);

        encoder.askBookCount(4)
                .next().price(98).size(100L)
                .next().price(101L).size(200L)
                .next().price(110L).size(5000L)
                .next().price(119L).size(5600L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }
    protected UnsafeBuffer createTick3() { // allows for 2 matches
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
                .next().price(105L).size(100L)
                .next().price(103L).size(300L)
                .next().price(101L).size(400L);

        encoder.askBookCount(3)
                .next().price(98).size(200L)
                .next().price(110L).size(300L)
                .next().price(115L).size(5000L);


        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }
    protected UnsafeBuffer createTick4() {  // allows for 3 matches
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
                .next().price(108L).size(200L)
                .next().price(105L).size(300L)
                .next().price(103L).size(400L);

        encoder.askBookCount(3)
                .next().price(98).size(300L)
                .next().price(112L).size(200L)
                .next().price(115L).size(5000L);


        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer finalMarketTickOverExecution() {  // allows for 4 matches to test over-execution
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
                .next().price(108L).size(200L)
                .next().price(105L).size(300L)
                .next().price(103L).size(400L);

        encoder.askBookCount(3)
                .next().price(98).size(400L)
                .next().price(112L).size(200L)
                .next().price(115L).size(5000L);


        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTickBuyLow1(){
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
                .next().price(93L).size(100L)
                .next().price(92L).size(100L)
                .next().price(90L).size(100L);
        encoder.askBookCount(3)
                .next().price(102L).size(100L)
                .next().price(103L).size(200L)
                .next().price(105).size(300L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }
    protected UnsafeBuffer createTickBuyLow2(){
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
                .next().price(94L).size(100L)
                .next().price(93L).size(200L)
                .next().price(92L).size(100L);
        encoder.askBookCount(3)
                .next().price(98L).size(100L)
                .next().price(99L).size(100L)
                .next().price(100L).size(400L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }
    protected UnsafeBuffer createTickBuyLow3(){
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
                .next().price(95L).size(100L)
                .next().price(94L).size(200L)
                .next().price(93L).size(200L);
        encoder.askBookCount(3)
                .next().price(96L).size(200L)
                .next().price(97L).size(300L)
                .next().price(98).size(400L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTickFillBUYOrders(){
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
                .next().price(95L).size(100L)
                .next().price(94L).size(100L)
                .next().price(93L).size(200L);
        encoder.askBookCount(3)
                .next().price(94L).size(400L)
                .next().price(97L).size(300L)
                .next().price(98).size(400L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    protected UnsafeBuffer createTickFillSELLOrders(){
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
                .next().price(93L).size(100L)
                .next().price(92L).size(100L)
                .next().price(90L).size(100L);
        encoder.askBookCount(3)
                .next().price(104L).size(100L)
                .next().price(105L).size(200L)
                .next().price(106).size(300L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

}