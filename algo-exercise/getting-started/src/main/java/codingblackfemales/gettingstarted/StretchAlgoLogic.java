package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
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
import org.slf4j.LoggerFactory;import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/* LOGIC: This Algo logic builds on the basic algo logic by
    * Adding orders on the BUY side when favours buying low (sellers are placing lower ask offers than historic data) OR when the market favours selling at a higher price (ask price are going back up), place a SELL order that purchases those 300 shares previously bought at a higher price or vice versa to ensure a profit can be made.
 * The Market trend is determined using the Moving Weight Average strategy by:
     * 1. An average of each instance of the order book is calculated over 6 occurrences to ensure an accurate trend of the market is captured
     * 2. The sum of those 6 averages are calculated and then added up
     * 3. If the ask side trend demonstrates a strong decline --> BUY to secure a security at a cheaper price
     * 4. If the bid side shows an increasing trend and the ask side shows an increasing trend -->  SELL those previously acquired shares at a higher price
 * Assumptions for this logic:
     * We are either provided with a market order to BUY 300 shares or SELL 300 shares by sending 3 child orders
     * If the trend favours BUYING cheap, SELL high for later or vice versa
     * Orders that are not filled are cancelled by end of Day OR if the market is closed [Public holidays have not been accounted for] - no orders are placed * */


public class StretchAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoLogic.class);
    private MarketStatus marketStatus = new MarketStatus();
    private MovingWeightAverageCalculator mwaCalculator;

    final int MINIMUM_ORDER_BOOKS = 6; //
    final int MAX_CHILD_ORDERS = 3;
    final long childOrderQuantity = 100;
    long parentOrderQuantity = 300; // buy or sell 300 shares (3 child orders of a 100)
    final double TREND_THRESHOLD = 0.5; // use this threshold to avoid algo reacting to small fluctuations in price - prices can only be long so 0.5 is a good measure for a stable market
    // initialise the moving averages list as instance fields to allow the evolute method to accumulate them over several ticks
    private List<Double> bidAverages = new ArrayList<>();
    private List<Double> askAverages = new ArrayList<>();


    public StretchAlgoLogic(MarketStatus marketStatus) {
        this.marketStatus = marketStatus;
    }

    public boolean isMarketClosed() {
        return marketStatus.isMarketClosed();
    }

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[STRETCH-ALGO] The state of the order book is:\n{}", orderBookAsString);

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        long totalFilledQuantity = allChildOrders.stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of quantities of all filled orders
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // list of all child orders (active and non-active)

        // Exit Condition 1: If Market is closed before logic is triggered - don't return any action
        if(marketStatus.isMarketClosed() && allChildOrders.isEmpty()) {
            logger.info("[STRETCH-ALGO] No orders on the market & Market is closed, Not placing new orders ");
            return NoAction.NoAction;
        } else{
            logger.info("[STRETCH-ALGO] The market is NOT closed, continuing with logic");
        }

        // Exit Condition 2: If Market is closed after logic has been triggered - cancel all non-filled orders on the market
        List<ChildOrder> ordersToCancel = activeChildOrders.stream().filter(childOrder -> childOrder.getFilledQuantity() == 0).toList();
        if (marketStatus.isMarketClosed() && !ordersToCancel.isEmpty()){
            for (ChildOrder orderToCancel: ordersToCancel){
                logger.info("[STRETCH-ALGO] The market is closed. Cancelling day order ID: {} on side: {}", orderToCancel.getOrderId(), orderToCancel.getSide());
                return new CancelChildOrder(orderToCancel);
            }
        } else{
            logger.info("[STRETCH-ALGO] The market is NOT closed and there are no orders to cancel, continuing with logic");
        }

        // Exit Condition 3: Return No action if max count of child orders created or parent order filled (all 3 child orders)
        if (allChildOrders.size() >= MAX_CHILD_ORDERS || totalFilledQuantity >= parentOrderQuantity) {
            logger.info("[STRETCH-ALGO] Maximum number of orders: {} reached OR parent desired quantity has been filled {}. Returning No Action.", allChildOrders.size(), totalFilledQuantity);
            return NoAction.NoAction;
        } else{
            logger.info("[STRETCH-ALGO] Max number of children yet to be created OR total filled quantity yet to be reached, continuing with logic");
        }

        // 1. calc the average for each order book --> add that to a list --> add to a list of moving averages --> once list.size() == 6 --> evaluate trend -->
        List<OrderBookLevel> bidLevels = getOrderBookLevels(state).get("Bid");
        List<OrderBookLevel> askLevels = getOrderBookLevels(state).get("Ask");
        double bidMovingWeightAverage = calculateMovingWeightAverage(bidLevels);
        double askMovingWeightAverage = calculateMovingWeightAverage(askLevels);
        bidAverages.add(bidMovingWeightAverage);
        askAverages.add(askMovingWeightAverage);

        if (bidAverages.size() < MINIMUM_ORDER_BOOKS || askAverages.size() < MINIMUM_ORDER_BOOKS) {
            logger.info("[STRETCH-ALGO] Insufficient Moving weight averages to evaluate the market trend, there are currently {} bids averages and {} asks averages", bidAverages.size(), askAverages.size());
            return NoAction.NoAction;
        }

        logger.info("[STRETCH-ALGO] Enough moving weight averages to evaluate market trend, calculating market trend for both sides");
        double bidMarketTrend = evaluateTrendUsingMWAList(bidAverages);
        double askMarketTrend = evaluateTrendUsingMWAList(askAverages);
        logger.info("[STRETCH-ALGO] Market trend calculated, Bid market trend is {}, Ask Market Trend is {}", bidMarketTrend, askMarketTrend);

        logger.info("[STRETCH-ALGO] FilledQuantity Tracker: Total Filled Quantity for orders so far is: {}", totalFilledQuantity);

        if (askMarketTrend < - TREND_THRESHOLD) { // buy if sellers are offering lower and lower prices
            logger.info("[STRETCH-ALGO] Trend favorable for BUY, placing child order.");
            final BidLevel bestBid = state.getBidAt(0);
            long price = bestBid.price;
            logger.info("Best bid: {}", bestBid);
            return new CreateChildOrder(Side.BUY, childOrderQuantity, price);
        } else if (askMarketTrend >  TREND_THRESHOLD && bidMarketTrend > TREND_THRESHOLD) { // sell what you originally bought for more by checking ask side trend
            logger.info("[STRETCH-ALGO] Trend favorable for SELL, placing child order.");
            final AskLevel bestAsk = state.getAskAt(0);
            long price = bestAsk.price;
            return new CreateChildOrder(Side.SELL, childOrderQuantity, price);
        } else if (Math.abs(bidMarketTrend) <= TREND_THRESHOLD && Math.abs(bidMarketTrend) <= TREND_THRESHOLD) { // if both sides trend shows little to no fluctuation, don't place orders
            logger.info("[STRETCH-ALGO] Market is stable, holding off placing orders for the meantime. Returning No Action.");
            return NoAction.NoAction;
        } // // profit == bestBid - lowest ask offer

        logger.info("[STRETCH-ALGO] No action to take.");
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

    public static class OrderBookLevel {
        public final long price;
        public final long quantity;

        public OrderBookLevel(long price, long quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }

    // Method 3: Evaluate trend based on the list of averages we have on the most recent 6 instances of historical data
    public double evaluateTrendUsingMWAList(List<Double> listOfAverages) {
        double sumOfDifferences = 0;

        if (!listOfAverages.isEmpty()) {
            for (int i = 0; i < listOfAverages.size() - 1; i++) {
                double differenceInTwoAverages = listOfAverages.get(i + 1) - listOfAverages.get(i);
                sumOfDifferences += differenceInTwoAverages;
            }
        }
        return sumOfDifferences;
    }

    public double calculateMovingWeightAverage(List<OrderBookLevel> OrderBookLevel) {
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for (OrderBookLevel order : OrderBookLevel) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round((weightedSum / totalQuantityAccumulated) * 100.0) / 100.0;
        return totalQuantityAccumulated > 0 ? weightedAverage : 0;
    }
}