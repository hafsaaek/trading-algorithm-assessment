package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;

import static java.lang.Math.toIntExact;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc..)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */
public class MyAlgoTest extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic();
    }


    @Test
    public void testDispatchThroughSequencer() throws Exception {

        //create a sample market data tick....
        send(createTick());

        //simple assert to check we have 1 market
        assertEquals(1, container.getState().getChildOrders().size());
        // assert to check for order quantity 
        assertEquals(100, container.getState().getActiveChildOrders().get(0).getQuantity());

    }

    @Test
    public void testFirstOrderCancellation() throws Exception {
        //create a sample market data tick....
        send(createTick());

        ChildOrder firstChildOrder = container.getState().getActiveChildOrders().get(0);
        firstChildOrder.setState(OrderState.FILLED);

        send(createTick()); // re-evaluate the logic
        assertEquals(1, container.getState().getActiveChildOrders().size()); // Ensure one is cancelled so the number of active returned is 2 - an indirect check because you're asusming other copmonents are workign jsut fine when you haven't tested it

        assertEquals(OrderState.CANCELLED, container.getState().getChildOrders().get(0).getState()); // Ensure one is cancelled so the number of active returned is 2
    }



    @Test 
    public void testFilledOrderQuantityAccumulation() throws Exception {
        // Test for totalFilledQuantity & placedQuanity accumulation once an order is filled

        // send a tick i.e. a mock market update to trigger the algorithm
        send(createTick());

        // mock a case where the first child order is filled by setting the state == filled & manually set it's fileld quanitity
        ChildOrder firstChildOrder = container.getState().getActiveChildOrders().get(0);
        firstChildOrder.setState(OrderState.FILLED);
        firstChildOrder.addFill(100, 10); // does it matter the order of how this is defined?

        // Trigger the algo logic evaluation again by sending a new tick
        send(createTick());

        long totalFilledQuantity = firstChildOrder.getFilledQuantity();
        long placedQuanity = firstChildOrder.getQuantity() - firstChildOrder.getFilledQuantity();

        assertEquals(100, totalFilledQuantity);
        assertEquals(0, placedQuanity);

    }

    @Test
    public void testPartialFillTriggersNewOrderForRemainingQuantity() throws Exception {
        send(createTick());

        ChildOrder partialFilleOrder = container.getState().getActiveChildOrders().get(0);
        partialFilleOrder.addFill(250, 10); 
        partialFilleOrder.setState(OrderState.FILLED);


        send(createTick());
        ChildOrder partialNonFilledOrder = container.getState().getActiveChildOrders().get(0);


        assertEquals(250,partialFilleOrder.getFilledQuantity()); // check 250 shares have been bought
        assertEquals(OrderState.CANCELLED, partialFilleOrder.getState()); 

        // check another order with amount 50 has been put on the market with 0 filled quantity
        assertEquals(0,partialNonFilledOrder.getFilledQuantity()); 
        assertEquals(50,partialNonFilledOrder.getQuantity());

    }





    // @Test // Test if there are more than 3 orders with state Filled - no more chid orders are made 
    // public void testOverExecution() throws Exception {
    //     send(createTick());
    //     List<ChildOrder> filledOrders= new ArrayList<>(); 
    //     for (ChildOrder childOrder : container.getState().getActiveChildOrders()) {
    //         // childOrder.setState(OrderState.FILLED);
    //         filledOrders.add(childOrder);
    //     }
    
    //     int nonFilledOrdersCount = container.getState().getActiveChildOrders().size() - filledOrders.size();

    //     assertEquals(3, container.getState().getChildOrders().size()); 

    //     assertEquals(0, nonFilledOrdersCount); 

    // }

}

// ./mvnw clean test --projects algo-exercise/getting-started -Dtest=codingblackfemales.gettingstarted.MyAlgoTest > test-results.txt

// patial orders
// states - 
// hit every lien in the logic 
// tip: if you keep seeing errors such as " The public type MessageHeaderDecoder must be defined in its own file" - this is porbably because there are several compiled java classes of the same file e.g. DefaultSequencer 3 and DefaultSequencer 2- clean install using maven to refresh the project (mvn clean install) - best to commentout any tests that might not be workign to not disrupt the build