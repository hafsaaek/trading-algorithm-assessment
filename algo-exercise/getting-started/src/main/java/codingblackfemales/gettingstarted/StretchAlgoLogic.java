package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StretchAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {
        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[STRETCH-ALGO] The state of the order book is:\n{}", orderBookAsString);
        /* Money Making Logic:
         * 1. Calculate the weighted average of the order book as new orders come in
         * 2. Compute the difference between weighted averages as orders come in -
         * If the momentum (weight average difference) points to an INCREASING trend --> Order book is becoming more expensive thus place a BUY order to secure cheaper bid price
         * If the momentum (weight average difference) points to an DECREASING trend --> Order book is getting cheaper thus place a SELL order to secure higher ask price
         * Note: if trend is increasing (avg diff > 0) --> we want to buy at the best bid price because other ask orders are expected to be more expensive
         * Would EMA be better?
         * Question: would you not want to place orders right away - when the market is fresh and new, this method is dis-advantageous because it might take some time to calculate trends before
         * this algo also assumes we have roughly equal bid & ask orders
         */

        List<OrderBookLevel> marketOrders = getListOfOrderLevels(state);

        // get the moving averages
        List<Double> movingAverageList = calculateWeightedMovingAverage(state);
        // calculate the market trend by calling
        double marketTrend = evaluateMarketTrend(movingAverageList);

        final int MINIMUM_ORDER_COUNT_FOR_MARKET_TREND_EVALUATION = 6; // Arbitrary level to ensure there are e.g., 3 asks and 3 bids
        long childOrderQuantity = 100;

        if (marketOrders.size() >= MINIMUM_ORDER_COUNT_FOR_MARKET_TREND_EVALUATION){
            logger.info("[STRETCH-ALGORITHM] We have enough orders {} to evaluate the market trend", marketOrders.size());
            final BidLevel bestBid = state.getBidAt(0);
            final AskLevel bestAsk = state.getAskAt(0);
            if (marketTrend > 0) {
                logger.info("[STRETCH-ALGORITHM] Prices are expected to increase as WMA is more than 0, best time to place a BUY order. Placing a Child order");
                long price = bestBid.price;
                return new CreateChildOrder(Side.BUY, childOrderQuantity, price);
            } else if (marketTrend < 0) {
                logger.info("[STRETCH-ALGORITHM] Prices are expected to fall as WMA is less than 0, best time to place a SELL order. Placing a Child order");
                long price = bestAsk.price;
                return new CreateChildOrder(Side.SELL, childOrderQuantity, price);
            } else {
                logger.info("[STRETCH-ALGORITHM] Market is stable, holding off placing orders for the meantime");
                return NoAction.NoAction;
            }
        } else {
            logger.info("[STRETCH-ALGORITHM] Order book does not contain enough orders to evaluate market trend, waiting for more orders");
            return NoAction.NoAction;
        }
    }


    public List<OrderBookLevel> getListOfOrderLevels(SimpleAlgoState state) {
        List<OrderBookLevel> orderLevelsList = new ArrayList<>(); // initialise an empty list of orderLevel Objects

        int maxCountOfLevels = Math.max(state.getAskLevels(), state.getBidLevels()); // get max number of levels in order book

        for (int i =0; i < maxCountOfLevels; i++) {
            if (state.getBidLevels() > i) { // if there are bid orders --> get the first level price & quantity
                BidLevel bidLevel =  state.getBidAt(i);
                orderLevelsList.add(new OrderBookLevel(bidLevel.price, bidLevel.quantity)); // Create a new OrderBookLevel for bid side
            }
            if (state.getAskLevels() > i) { // if there are ask orders --> get the first level price & quantity
                AskLevel askLevel =  state.getAskAt(i);
                orderLevelsList.add(new OrderBookLevel(askLevel.price, askLevel.quantity)); // Create a new OrderBookLevel for ask side
            }
        }
      return orderLevelsList;
    }

    public class OrderBookLevel{
        public final long price;
        public final long quantity;

        public OrderBookLevel(long price, long quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    public List<Double> calculateWeightedMovingAverage(SimpleAlgoState state){
        List<Double> movingAverageList = new ArrayList<>(); // list to store moving averages as orders are placed on the market
        // Collections work with reference types not primitive data (i.e. Double uses here instead of double)
        List<OrderBookLevel> orderLevels = getListOfOrderLevels(state); // get order levels

        long totalQuantityAccumulated = 0;
        double weightedSum = 0;

        for(OrderBookLevel orderLevel : orderLevels) { // loop through the order levels to calculate WMA
            totalQuantityAccumulated += orderLevel.quantity;
            weightedSum += (orderLevel.price * orderLevel.quantity);
            // Calculate weighted average after each new order comes in
            if (totalQuantityAccumulated > 0) {
                double weightedAverage = weightedSum / totalQuantityAccumulated;
                movingAverageList.add(weightedAverage);
            }
        }
        System.out.println(movingAverageList);
        return movingAverageList;
    }

    // Method to determine market trend by comparing weighted averages between orders
    public double evaluateMarketTrend(List<Double> movingAverageList){
        double movingAverageDifference;
        int movingAverageCount = movingAverageList.size();
        movingAverageDifference = movingAverageList.get(movingAverageCount - 1) - movingAverageList.get(movingAverageCount - 2);

        return movingAverageDifference;
    }


    // Feedback:
        // Separate the calculation of bids & asks averages
        // Think about exceptions thrown when declaring
        // How do you keep account of profit!!
            // maybe by keeping track of what
        // actually test : unit test for calculation of averages and back tests for the creation of orders
        // limit orders on the market to a value too

}
