package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OldStretchAlgoLogic implements AlgoLogic {
    final int MINIMUM_BOOK_LEVELS = 6; //
    final int MAX_CHILD_ORDERS = 3;
    long childOrderQuantity = 100;
    long parentOrderQuantity = 300; // buy or sell 300
    final double TREND_THRESHOLD = 0.5; // use this threshold to avoid algo reacting to small fluctuations in price - prices can only be long so 0.5 is a good measure for a stable market
    final long SPREAD_THRESHOLD = 3; // hardcoded spread threshold

    private static final Logger logger = LoggerFactory.getLogger(OldStretchAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {
        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[STRETCH-ALGO] The state of the order book is:\n{}", orderBookAsString);

        /* Money Making Logic:
         * 1. Calculate the weighted average of the order book as new orders come in - we need minimum 6 averages calculated for ech side
         * 2. Compute the difference between weighted averages for each side
         * 3. If the momentum on the bid side  points to an INCREASING trend --> Order book is becoming more expensive thus place a BUY order to secure cheaper bid price
         * 4. If the momentum on the ask side  points to an DECREASING trend --> Order book is getting cheaper thus place a SELL order to secure higher ask price
         * 4. If there is no momentum on either side or the spread is too large --> Return No action  */

        List<OrderBookLevel> bidLevels = getOrderBookLevels(state).get("Bid");
        List<OrderBookLevel> askLevels = getOrderBookLevels(state).get("Ask");
        final BidLevel bestBid = state.getBidAt(0);
        final AskLevel bestAsk = state.getAskAt(0);

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        long totalFilledQuantity = allChildOrders.stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of quantities of all filled orders

        // Return No action if max count of child orders created or parent order filled
        if (allChildOrders.size() >= MAX_CHILD_ORDERS || totalFilledQuantity >= parentOrderQuantity) {
            logger.info("[STRETCH-ALGO] Maximum number of orders: {} reached OR parent desired quantity has been filled {}. Returning No Action.", allChildOrders.size(), totalFilledQuantity);
            return NoAction.NoAction;
        }

        // Logic to either sell or buy: buy if MWA diff is stronger than that on the sell side, vice versa for ask side. Otherwise, do nothing if there isn't much fluctuation or the spread is too high
        if (bidLevels.size() >= MINIMUM_BOOK_LEVELS && askLevels.size() >= MINIMUM_BOOK_LEVELS) {
            logger.info("[STRETCH-ALGO] We have {} bids and {} asks to evaluate the market trend", bidLevels.size(), askLevels.size());
            logger.info("[MY-ALGO] FilledQuantity Tracker: Total Filled Quantity for orders so far is: {}", totalFilledQuantity);
            // get the trend from the weighted moving averages differences
            double bidMarketTrend = evaluateTrendUsingMWA("Bid", bidLevels);
            double askMarketTrend = evaluateTrendUsingMWA("Ask", askLevels);
            final long bestBidPrice = bidLevels.stream().mapToLong(level -> level.price).max().orElse(0);
            final long lowestBidOffer = bidLevels.stream().mapToLong(level -> level.price).min().orElse(0);
            final long bestAskPrice = askLevels.stream().mapToLong(level -> level.price).min().orElse(0);
            final long highestAskOffer = askLevels.stream().mapToLong(level -> level.price).max().orElse(0);
            logger.info("[STRETCH-ALGO] best bid: {}, lowestBidOffer: {} bestAsk:{}, highestAskOffer: {}", lowestBidOffer, bestBidPrice, bestAskPrice, highestAskOffer);
            long orderBookSpread = bestAskPrice - bestBidPrice;
            if (orderBookSpread >= SPREAD_THRESHOLD) {
                logger.info("[STRETCH-ALGO] Order book spread is currently {} which is equal to or larger than SPREAD_THRESHOLD. Market too volatile to place an order, Returning No Action.", orderBookSpread);
                return NoAction.NoAction;
            } // We are buying when bid prices are expected to increase and ask prices are falling (to allow us to buy cheap)
            // profit == bestBid - lowest ask offer
            else if (bidMarketTrend > TREND_THRESHOLD && askMarketTrend < TREND_THRESHOLD) {
                logger.info("[STRETCH-ALGO] Prices are expected to increase as WMA is more than 0, best time to place a BUY order. Placing a Child order");
                long price = bestBid.price;
                return new CreateChildOrder(Side.BUY, childOrderQuantity, price);
            } // We will sell when bid prices are increasing (to sell high) and ask
            else if (askMarketTrend > TREND_THRESHOLD && askMarketTrend > bidMarketTrend) {
                logger.info("[STRETCH-ALGO] Prices are expected to fall as WMA is less than 0, best time to place a SELL order. Placing a Child order");
                long price = bestAsk.price;
                return new CreateChildOrder(Side.SELL, childOrderQuantity, price);
            }  // introduce a condition to account for when the market is stable
            else if (bidMarketTrend < TREND_THRESHOLD && askMarketTrend < TREND_THRESHOLD) { // stable criteria
                logger.info("[STRETCH-ALGO] Market is stable, holding off placing orders for the meantime. Returning No Action.");
                return NoAction.NoAction;
            }
        } else {
            logger.info("[STRETCH-ALGO] We do not have enough orders to evaluate the market trend, there are currently {} bids and {} asks", bidLevels.size(), askLevels.size());
        }

        return NoAction.NoAction;
    }

    public HashMap<String, List<OrderBookLevel>> getOrderBookLevels(SimpleAlgoState state) {
        List<OrderBookLevel> bidMarketOrders = new ArrayList<>(); // initialise an empty list of orderLevel Objects
        List<OrderBookLevel> askMarketOrders = new ArrayList<>(); // initialise an empty list of orderLevel Objects

        int maxCountOfLevels = Math.max(state.getAskLevels(), state.getBidLevels()); // get max number of levels in order book

        for (int i = 0; i < maxCountOfLevels; i++) {
            if (state.getBidLevels() > i) { // if there are bid orders --> get the first level price & quantity
                BidLevel bidLevel = state.getBidAt(i);
                bidMarketOrders.add(new OrderBookLevel(bidLevel.price, bidLevel.quantity)); // Create a new OrderBookLevel for bid side
            }
            if (state.getAskLevels() > i) { // if there are ask orders --> get the first level price & quantity
                AskLevel askLevel = state.getAskAt(i);
                askMarketOrders.add(new OrderBookLevel(askLevel.price, askLevel.quantity)); // Create a new OrderBookLevel for ask side
            }
        }
        // to allow us to return both list sin the same method - a hashmap will be used (time complexity of o(1) - easier to lookup
        HashMap<String, List<OrderBookLevel>> orderBookMap = new HashMap<>();
        orderBookMap.put("Bid", bidMarketOrders);
        orderBookMap.put("Ask", askMarketOrders);

        return orderBookMap;
    }

    public class OrderBookLevel {
        public final long price;
        public final long quantity;

        public OrderBookLevel(long price, long quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    public double evaluateTrendUsingMWA(String side, List<OrderBookLevel> ordersList) {
        if (ordersList.isEmpty()) {
            logger.warn("[STRETCH-ALGO] No {} levels available.", side);
        }

        // Get the weighted moving averages
        List<Double> movingAverages = getMovingAverages(ordersList);
        System.out.printf("Moving averages list: %s on %s side%n", movingAverages, side);

        if (movingAverages.size() < MINIMUM_BOOK_LEVELS) { // Ensures we have 5 differences to evaluate trend
            logger.warn("[STRETCH-ALGO] Insufficient moving averages to calculate trend on the {} side.", side);
            return 0.0;  // No trend
        }
        double sumOfDifferences = 0;
        for (int i = 0; i < movingAverages.size() - 1; i++) {
            double differenceInTwoAverages = movingAverages.get(i + 1) - movingAverages.get(i);
            sumOfDifferences += differenceInTwoAverages;
        }
        return sumOfDifferences; // return weighted average or 0 if totalQuantityAccumulated =< 0
    }

    private List<Double> getMovingAverages(List<OrderBookLevel> ordersList) {
        List<Double> movingAverages = new ArrayList<>();
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        for (OrderBookLevel order : ordersList) { // loop through the order levels to calculate WMA
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
            if (totalQuantityAccumulated > 0) {
                double weightedAverage = Math.round((weightedSum / totalQuantityAccumulated) * 100.0) / 100.0;
                movingAverages.add(weightedAverage);
            }
        }
        return movingAverages;
    }


}