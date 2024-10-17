package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyAlgo2Test extends AbstractAlgoTest {

    private MarketStatus marketStatus;

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        marketStatus = mock(MarketStatus.class);
        return new MyAlgoLogic2(marketStatus);
    }

    MyAlgoLogic algoLogic = new MyAlgoLogic();

    @Test
    public void testDispatchThroughSequencer() throws Exception {
        // check no orders on the market before sending an update
        assertTrue(container.getState().getChildOrders().isEmpty());

        // simulate the market being open
        when(marketStatus.isMarketOpen()).thenReturn(true);
        //create a sample market data tick....
        send(createTick());
        //simple assert to check we had 3 orders created, all active
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        send(createTick2());
        assertEquals(3, container.getState().getChildOrders().size());

        // simulate the market closing to check we have cancelled active orders
        when(marketStatus.isMarketOpen()).thenReturn(false);
        send(createTick2());
        assertEquals(3, container.getState().getChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());

        send(createTick());
        Action returnAction = algoLogic.evaluate(container.getState());
        assertEquals(NoAction.class, returnAction.getClass());

    }
}