package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

        //simple assert to check we had 3 orders created
        assertEquals(container.getState().getChildOrders().size(), 3);
    }

    @Test
    public void testAnOrderCreated() throws Exception {

        //create a sample market data tick....
        send(createTick());

        //simple assert to check we had 3 orders created
        assertEquals(container.getState().getActiveChildOrders().size(), 3);
    }


    @Test
    public void testFirstOrderCancellation() throws Exception {
        //create a sample market data tick....
        send(createTick());

        ChildOrder firstChildOrder = container.getState().getActiveChildOrders().get(0);
        firstChildOrder.setState(OrderState.FILLED);

        send(createTick());

        // a new order would be created by now by logic as it always tries to create 3 orders ...
        assertEquals(3, container.getState().getActiveChildOrders().size());

        // we look explicitly for canceled orders here
        var firstCanceledChild = container.getState().getChildOrders().stream().filter(childOrder -> childOrder.getState() == OrderState.CANCELLED).findFirst().orElse(null);
        assertNotNull(firstCanceledChild);
        assertEquals(2, firstCanceledChild.getOrderId());

    }

}

// ./mvnw clean test --projects algo-exercise/getting-started -Dtest=codingblackfemales.gettingstarted.MyAlgoTest > test-results.txt
