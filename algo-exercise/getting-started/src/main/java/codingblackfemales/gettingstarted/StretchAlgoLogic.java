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

public class StretchAlgoLogic implements AlgoLogic {
    final int MINIMUM_ORDER_BOOKS = 6; //
    final int MAX_CHILD_ORDERS = 3;
    long childOrderQuantity = 100;
    long parentOrderQuantity = 300; // buy or sell 300 shares (3 child orders of a 100)
    final double TREND_THRESHOLD = 0.5; // use this threshold to avoid algo reacting to small fluctuations in price - prices can only be long so 0.5 is a good measure for a stable market
    final long SPREAD_THRESHOLD = 3; // hardcoded spread threshold
    // initialise the moving averages list as instance fields to allow the evolute method to accumulate them over several ticks
    private List<Double> bidAverages = new ArrayList<>();
    private List<Double> askAverages = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[STRETCH-ALGO] The state of the order book is:\n{}", orderBookAsString);

        /* Money Making Logic:
         * 1. Calculate the weighted average of 6 order books as new orders come in - we need minimum 6 averages calculated for ech side
         * 2. Compute the difference between weighted averages for each side
         * 3. If the momentum on the bid side  points to an INCREASING trend --> Order book is becoming more expensive thus place a BUY order to secure cheaper bid price
         * 4. If the momentum on the ask side  points to an DECREASING trend --> Order book is getting cheaper thus place a SELL order to secure higher ask price
         * 5. If there is no momentum on either side or the spread is too large --> Return No action  */

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        long totalFilledQuantity = allChildOrders.stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of quantities of all filled orders

        // Return No action if max count of child orders created or parent order filled
        if (allChildOrders.size() >= MAX_CHILD_ORDERS || totalFilledQuantity >= parentOrderQuantity) {
            logger.info("[STRETCH-ALGO] Maximum number of orders: {} reached OR parent desired quantity has been filled {}. Returning No Action.", allChildOrders.size(), totalFilledQuantity);
            return NoAction.NoAction;
        }

        // 1. calc the average for each order book --> add that to a list --> add to a list of moving averages --> once list.size() == 6 --> evaluate trend -->
        List<OrderBookLevel> bidLevels = getOrderBookLevels(state).get("Bid");
        List<OrderBookLevel> askLevels = getOrderBookLevels(state).get("Ask");
        double bidMovingWeightAverage = calculateMovingWeightAverage(bidLevels);
        double askMovingWeightAverage = calculateMovingWeightAverage(askLevels);
//        bidAverages.add(bidMovingWeightAverage);
//        askAverages.add(askMovingWeightAverage);

        if(bidAverages.size() < MINIMUM_ORDER_BOOKS || askAverages.size() < MINIMUM_ORDER_BOOKS){
            logger.info("[STRETCH-ALGO] Insufficient Moving weight averages to evaluate the market trend, there are currently {} bids averages and {} asks averages", bidAverages.size(), askAverages.size());
            // append the averages list until we have 6 averages
            bidAverages.add(bidMovingWeightAverage);
            askAverages.add(askMovingWeightAverage);
            return NoAction.NoAction;

        } else {
            // Logic to either sell or buy: buy if MWA diff is stronger than that on the sell side, vice versa for ask side. Otherwise, do nothing if there isn't much fluctuation or the spread is too high
            logger.info("[STRETCH-ALGO] Enough moving weight averages to evaluate market trend, calculating market trend for both sides");
            double bidMarketTrend = evaluateTrendUsingMWAList(bidAverages);
            double askMarketTrend = evaluateTrendUsingMWAList(askAverages);
            logger.info("[STRETCH-ALGO] Market trend calculated, Bid market trend is {}, Ask Market Trend is {}", bidMarketTrend, askMarketTrend);
            logger.info("[MY-ALGO] FilledQuantity Tracker: Total Filled Quantity for orders so far is: {}", totalFilledQuantity);

            final long lowestBid = bidLevels.stream().mapToLong(level -> level.price).min().orElse(0);
            final long highestBid = bidLevels.stream().mapToLong(level -> level.price).max().orElse(0);
            final long lowestAsk = askLevels.stream().mapToLong(level -> level.price).min().orElse(0);
            final long highestAsk = askLevels.stream().mapToLong(level -> level.price).max().orElse(0);
            logger.info("[STRETCH-ALGO]  Lowest bid: {}, Highest bid: {} lowest ask:{}, highestAskOffer: {}", lowestBid, highestBid, lowestAsk, highestAsk);

            long orderBookSpread = highestBid - lowestAsk;
            logger.info("[STRETCH-ALGO] Bid WMA: {}, Ask WMA: {}", bidMarketTrend, askMarketTrend);
            // hold off if the market is volatile and there is a large gap between bid and ask offers
            if (orderBookSpread >= SPREAD_THRESHOLD) {
                logger.info("[STRETCH-ALGO] Market is too volatile as Order book spread is too large: {}, Returning No Action.", orderBookSpread);
                return NoAction.NoAction;
            }
            // profit == bestBid - lowest ask offer
            // buy now before bid prices increase further and whilst ask side is offering lower and lower offers
            else if (bidMarketTrend > TREND_THRESHOLD && askMarketTrend < TREND_THRESHOLD) {
                logger.info("[STRETCH-ALGO] Trend favorable for BUY, placing child order.");
                final BidLevel bestBid = state.getBidAt(0);
                long price = bestBid.price;
                logger.info("Best bid: {}", price);
                return new CreateChildOrder(Side.BUY, childOrderQuantity, highestBid);
            } // For selling: short sell and sell at the current market value and then later buy it for less to make profit
            // so we're selling stock when it's more expensive to be able to buy it back at a cheaper price
            else if (askMarketTrend > TREND_THRESHOLD && askMarketTrend > bidMarketTrend) {
                logger.info("[STRETCH-ALGO] Trend favorable for SELL, placing child order.");
                long price = highestAsk;
                logger.info("Best ask: {}", price);
                return new CreateChildOrder(Side.SELL, childOrderQuantity, price);
            }  // introduce a condition to account for when the market is stable
            else if (bidMarketTrend <= TREND_THRESHOLD && askMarketTrend <= TREND_THRESHOLD) { // stable criteria
                logger.info("[STRETCH-ALGO] Market is stable, holding off placing orders for the meantime. Returning No Action.");
                return NoAction.NoAction;
            }
        }
        return NoAction.NoAction;
    }

    public HashMap<String, List<OrderBookLevel>> getOrderBookLevels(SimpleAlgoState state) {
        List<OrderBookLevel> bidMarketOrders = new ArrayList<>(); // initialise an empty list of orderLevel Objects
        List<OrderBookLevel> askMarketOrders = new ArrayList<>(); // initialise an empty list of orderLevel Objects

        int maxCountOfLevels = Math.max(state.getAskLevels(), state.getBidLevels()); // get max number of levels in order book

        for (int i =0; i < maxCountOfLevels; i++) {
            if (state.getBidLevels() > i) { // if there are bid orders --> get the first level price & quantity
                BidLevel bidLevel =  state.getBidAt(i);
                bidMarketOrders.add(new OrderBookLevel(bidLevel.price, bidLevel.quantity)); // Create a new OrderBookLevel for bid side
            }
            if (state.getAskLevels() > i) { // if there are ask orders --> get the first level price & quantity
                AskLevel askLevel =  state.getAskAt(i);
                askMarketOrders.add(new OrderBookLevel(askLevel.price, askLevel.quantity)); // Create a new OrderBookLevel for ask side
            }
        }
        // to allow us to return both list sin the same method - a hashmap will be used (time complexity of o(1) - easier to lookup
        HashMap<String, List<OrderBookLevel>> orderBookMap = new HashMap<>();
        orderBookMap.put("Bid", bidMarketOrders);
        orderBookMap.put("Ask", askMarketOrders);

        return orderBookMap;
    }

    public class OrderBookLevel{
        public final long price;
        public final long quantity;

        public OrderBookLevel(long price, long quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    // Method 3: Evaluate
    public double evaluateTrendUsingMWAList(List<Double> listOfAverages){
        double sumOfDifferences = 0;

        if(!listOfAverages.isEmpty()) {
            for(int i = 0; i < listOfAverages.size() - 1; i++){
                double differenceInTwoAverages = listOfAverages.get(i + 1) - listOfAverages.get(i);
                sumOfDifferences += differenceInTwoAverages;
            }
        }
        return sumOfDifferences;
    }

    private double calculateMovingWeightAverage(List<OrderBookLevel> OrderBookLevel) {
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage = 0;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for(OrderBookLevel order : OrderBookLevel) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round((weightedSum / totalQuantityAccumulated) * 100.0) / 100.0;
        return totalQuantityAccumulated >0 ? weightedAverage: 0;
    }


    // calculating the MWA for each isntance:
       // 1. calculate the MWA for each tick/orderbook for each side (method takes in an orderbook)
        // 2. calc the actual trend by:



    // To Do list
        // 1. Rewrite logic helper text DONE
        // 2. Separate method that calculates moving averages and overall trend DONE
        // 3. What to do if bid trend is increasing and ask is decreasing at the same rate? IG this is the same as widening spread so
        // 4. Think about profit tracking - what is the point of this:
//                - subtract orders that have been filled (what you sold - bought)
        // 5.
}

// Another approach: 2 algo's, one buys shares at a certain price, then a sell algo that sells the 300 shares you originally bought (limit the quantity to 300)