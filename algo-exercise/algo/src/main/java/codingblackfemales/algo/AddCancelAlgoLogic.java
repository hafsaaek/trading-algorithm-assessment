package codingblackfemales.algo;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddCancelAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(AddCancelAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        logger.info("[ADDCANCELALGO] In Algo Logic....");

        final String book = Util.orderBookToString(state);

        logger.info("[ADDCANCELALGO] Algo Sees Book as:\n{}", book);

        var totalOrderCount = state.getChildOrders().size();

        //make sure we have an exit condition...
        if (totalOrderCount > 20) {
            return NoAction.NoAction; // if there are more than 20 hild orders - do nothing 
        }
        logger.info("[ADDCANCELALGO] total order count:\n{}", totalOrderCount);

        final var activeOrders = state.getActiveChildOrders();

        if (!activeOrders.isEmpty()) {

            final var option = activeOrders.stream().findFirst(); 

            if (option.isPresent()) {
                var childOrder = option.get();
                if (logger.isInfoEnabled()) {
                    logger.info("[ADDCANCELALGO] Cancelling order:{}", childOrder);
                }
                return new CancelChildOrder(childOrder);
            }
            else{
                return NoAction.NoAction;
            }
            
        } else { // doesn't account for parent orders though  
            BidLevel level = state.getBidAt(0);
            final long price = level.price;
            final long quantity = 225;
            logger.info("[ADDCANCELALGO] Adding order for{}@{}", quantity, price);
            return new CreateChildOrder(Side.BUY, quantity, price);
        }


    }
}
