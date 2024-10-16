package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MyAlgo2Test extends AbstractAlgoTest {

    private MarketStatus marketStatusMock;

    @Override
    public AlgoLogic createAlgoLogic() {
        marketStatusMock = mock(MarketStatus.class);
        // this adds your algo logic to the container classes
        return new MyAlgoLogic2(marketStatusMock);
    }


    @Test
    public void testOrdersAreCreatedWhenMarketIsOpenAndCancelledOnMarketClosed() throws Exception {
        // check no orders on the market before sending an update
        assertTrue(container.getState().getChildOrders().isEmpty());

        // Given: market is open
        when(marketStatusMock.isMarketClosed()).thenReturn(false);

        // When: a tick is received
        send(createTick());

        // Then: 3 orders are created
        assertEquals(3, container.getState().getChildOrders().size());
        assertEquals(3, container.getState().getActiveChildOrders().size());

        // Given: market is closed
        when(marketStatusMock.isMarketClosed()).thenReturn(true);

        // When: a tick is received
        send(createTick2());

        // Then: all orders are cancelled
        assertEquals(3, container.getState().getChildOrders().size());
        assertTrue(container.getState().getActiveChildOrders().isEmpty());
    }

}