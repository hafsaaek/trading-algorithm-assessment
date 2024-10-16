package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;

import java.util.ArrayList;
import java.util.List;

public class OrderBookService {

    public List<OrderBookLevel> getBidLevels(SimpleAlgoState state) {
        List<OrderBookLevel> bidMarketOrders = new ArrayList<>();

        // Same logic as before for retrieving bids and asks
        for (int i = 0; i < state.getBidLevels(); i++) {
            if (state.getBidLevels() > i) {
                BidLevel bidLevel = state.getBidAt(i);
                bidMarketOrders.add(new OrderBookLevel(bidLevel.price, bidLevel.quantity));
            }
        }
        return bidMarketOrders;
    }

    public List<OrderBookLevel> getAskLevels(SimpleAlgoState state) {
        List<OrderBookLevel> askMarketOrders = new ArrayList<>();

        /* Same logic as before for retrieving asks */
        for (int i = 0; i < state.getAskLevels(); i++) {
            if (state.getAskLevels() > i) {
                AskLevel askLevel = state.getAskAt(i);
                askMarketOrders.add(new OrderBookLevel(askLevel.price, askLevel.quantity));
            }
        }
        return askMarketOrders;
    }

    public static class OrderBookLevel {
        public final long price;
        public final long quantity;

        public OrderBookLevel(long price, long quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }
}