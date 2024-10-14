package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyAlgo2Test extends AbstractAlgoTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic2();
    }

    private MyAlgoLogic2 myAlgoLogic2;
    private MyAlgoLogic2 myAlgoLogic2ForcedOpen;
    private MyAlgoLogic2 myAlgoLogic2ForcedClosed;

    @Before
    public void setUp(){
        myAlgoLogic2ForcedOpen = new MyAlgoLogic2() {
            @Override
            public boolean isMarketClosed(){
                return false;
            }
        };

        myAlgoLogic2ForcedClosed = new MyAlgoLogic2() {
            @Override
            public boolean isMarketClosed(){
                return true;
            }
        };
    }


    @Test
    public void testDispatchThroughSequencer() throws Exception {
        assertTrue(container.getState().getChildOrders().isEmpty());

        container.setLogic(myAlgoLogic2ForcedOpen);
        //create a sample market data tick....
        send(createTick());

        //simple assert to check we had 3 orders created
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        container.setLogic(myAlgoLogic2ForcedClosed);

        send(createTick2());
        assertEquals(3, container.getState().getChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());

    }
}