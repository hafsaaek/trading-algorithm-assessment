package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test plugs together all the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 *
 * If you cross the srpead (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object.
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 *
 */
public class StretchAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new StretchAlgoLogic();
    }
    private StretchAlgoLogic logicInstance;


    @BeforeEach
    public void setUp(){
        logicInstance = new StretchAlgoLogic();
    }

    @Test
    public void testIsMarketClosedFunction(){
        // need to create a situation where
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("Europe/London"));  // Declare market opening conditions
        LocalTime marketOpenTime = LocalTime.of(8, 0, 0);
        LocalTime marketCloseTime = LocalTime.of(16, 35, 00);

        // declare a boolean that will hold true for all market closed conditions (except holidays)
        boolean isMarketClosedTestVariable = timeNow.toLocalTime().isBefore(marketOpenTime) || timeNow.toLocalTime().isAfter(marketOpenTime) || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SATURDAY || timeNow.toLocalDate().getDayOfWeek() == DayOfWeek.SUNDAY;

        System.out.println(isMarketClosedTestVariable);
        System.out.println(logicInstance.isMarketClosed());

        // if boolean : true --> isMarketClosed() should also return true and vice versa
        assertTrue(isMarketClosedTestVariable == logicInstance.isMarketClosed());

    }

    @Test // Test that no orders are created with a stable market that has been forced open
    public void testStableMarketForcedOpen() throws Exception {
        StretchAlgoLogic marketIsForcedOpen = new StretchAlgoLogic() {
            @Override
            public boolean  isMarketClosed() {
                return false;
            }
        };
        container.setLogic(marketIsForcedOpen);
        //create a sample market data tick....
        send(createTick());
        send(createTick());
        send(createTick());
        send(createTick());
        send(createTick());
        send(createTick());
        Assert.assertEquals(0, container.getState().getChildOrders().size()); // Should be zero as we only have 5 averages
        send(createTick());
        Assert.assertEquals(0, container.getState().getChildOrders().size()); // Should be zero as market is stable

    }


    @Test
    public void testPlacingBuyingConditions() throws Exception {
        // send 6 market ticks to calculate 6 moving averages
//        send(createTick());
        send(createTickBuyLow1());
        send(createTickBuyLow1());
        send(createTickBuyLow2());
        send(createTickBuyLow2());
        send(createTickBuyLow3());
        send(createTickBuyLow3());
        // one last tick
        send(createTickBuyLow3());

        if(logicInstance.isMarketClosed()){
            assertTrue(container.getState().getChildOrders().isEmpty());
        } else {
            // Assert to check we have created 3 child orders under good buy conditions
            assertEquals(3, container.getState().getChildOrders().size());
            assertEquals(3, container.getState().getActiveChildOrders().size());
            assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("BUY")));
            assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == 100));

            // send a tick that would fill out orders
            send(createTickFillBUYOrders());

            //then: get the state
            var state = container.getState();

            //Check things like filled quantity, cancelled order count etc....
            long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
            //and: check that our algo state was updated to reflect our fills when the market data
            assertEquals(300, filledQuantity);
        }

        StretchAlgoLogic marketIsForcedOpen = new StretchAlgoLogic() {
            @Override
            public boolean  isMarketClosed() {
                return false;
            }
        };
        StretchAlgoLogic marketIsForcedClosed = new StretchAlgoLogic() {
            @Override
            public boolean  isMarketClosed() {
                return true;
            }
        };
        container.setLogic(marketIsForcedClosed);
        send(createTickFillBUYOrders());
        assertEquals(3, container.getState().getActiveChildOrders().size());

    }

    @Test
    public void testFilledOrdersAreNotCancelled() throws Exception{
        send(createTickBuyLow1());
        send(createTickBuyLow1());
        send(createTickBuyLow2());
        send(createTickBuyLow2());
        send(createTickBuyLow3());
        send(createTickBuyLow3());
        send(createTickBuyLow3());
        send(createTickFillBUYOrders());


        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = container.getState().getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);

    }

    @Test
    public void testPlacingSellingConditions() throws Exception {
        // send 6 market ticks to calculate 6 moving averages where ask offers are increasing
//        send(createTick());
        send(createTickBuyLow3());
        send(createTickBuyLow3());
        send(createTickBuyLow2());
        send(createTickBuyLow2());
        send(createTickBuyLow1());
        send(createTickBuyLow1());
        // one last tick
        send(createTickBuyLow1());

        // Assert to check we have created 3 child orders under good buy conditions
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getSide().toString().equals("SELL")));
        assertTrue(container.getState().getActiveChildOrders().stream().allMatch(childOrder -> childOrder.getQuantity() == 100));

        // send a tick that would fill out orders
        send(createTickFillSELLOrders());

        //then: get the state
        var state = container.getState();

        //Check things like filled quantity, cancelled order count etc....
        long filledQuantity = state.getChildOrders().stream().map(ChildOrder::getFilledQuantity).reduce(Long::sum).get();
        //and: check that our algo state was updated to reflect our fills when the market data
        assertEquals(300, filledQuantity);
    }

    // What's got me in a pickle:
    // 1. if i want to buy when it's cheap, i will also have to be able to match when sellers are willing to place lower ask offers
        // How will this affect my ability to sell again? --> I will hold on to my 300 stocks now and sell later when the market is more expensive
    // 2. Alternatively, I sell when it's more expensive because I now want to sell at a higher price than what i bought
    // profit: what did buy at first and then what did i sell at

}