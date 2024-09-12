package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;

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
        assertEquals(2, container.getState().getChildOrders().size()); // Ensure one is cancelled so the number of active returned is 2 - an indirect check because you're asusming other copmonents are workign jsut fine when you haven't tested it

        // check 

        assertEquals(OrderState.CANCELLED, container.getState().getChildOrders().get(0).getState()); // Ensure one is cancelled so the number of active returned is 2

    }

    // Decide where pt put logging 


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