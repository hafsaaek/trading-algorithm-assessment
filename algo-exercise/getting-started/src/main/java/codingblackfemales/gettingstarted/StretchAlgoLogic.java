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
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

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
    private  MarketStatus marketStatus;
    private MovingWeightAverageCalculator mwaCalculator ;
    private  OrderBookService orderBookService;

    public StretchAlgoLogic(MarketStatus marketStatus, OrderBookService orderBookService, MovingWeightAverageCalculator mwaCalculator) {
        this.orderBookService = orderBookService;
        this.marketStatus = marketStatus;
        this.mwaCalculator = mwaCalculator;
    }

    public boolean isMarketClosed() {
        return marketStatus.isMarketClosed();
    }

    final int MINIMUM_ORDER_BOOKS = 6; //
    final int MAX_CHILD_ORDERS = 3;
    final long childOrderQuantity = 100;
    long parentOrderQuantity = 300; // buy or sell 300 shares (3 child orders of a 100)
    final double TREND_THRESHOLD = 0.5; // use this threshold to avoid algo reacting to small fluctuations in price - prices can only be long so 0.5 is a good measure for a stable market
    private List<Double> bidAverages = new ArrayList<>();
    private List<Double> askAverages = new ArrayList<>();

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);
        logger.info("[STRETCH-ALGO] The state of the order book is:\n{}", orderBookAsString);

        List<ChildOrder> allChildOrders = state.getChildOrders(); // list of all child orders (active and non-active)
        long totalFilledQuantity = allChildOrders.stream().mapToLong(ChildOrder::getFilledQuantity).sum(); // sum of quantities of all filled orders
        List<ChildOrder> activeChildOrders = state.getActiveChildOrders(); // list of all child orders (active and non-active)

        /* Exit Condition 1: If Market is closed before logic is triggered - don't return any action */
        if(marketStatus.isMarketClosed() && allChildOrders.isEmpty()) {
            logger.info("[STRETCH-ALGO] No orders on the market & Market is closed, Not placing new orders ");
            return NoAction.NoAction;
        } else{
            logger.info("[STRETCH-ALGO] The market is NOT closed, continuing with logic");
        }

        /* Exit Condition 2: If Market is closed after logic has been triggered - cancel all non-filled orders on the market */
        List<ChildOrder> ordersToCancel = activeChildOrders.stream().filter(childOrder -> childOrder.getFilledQuantity() == 0).toList();
        if (marketStatus.isMarketClosed() && !ordersToCancel.isEmpty()){
            for (ChildOrder orderToCancel: ordersToCancel){
                logger.info("[STRETCH-ALGO] The market is closed. Cancelling day order ID: {} on side: {}", orderToCancel.getOrderId(), orderToCancel.getSide());
                return new CancelChildOrder(orderToCancel);
            }
        }

        /* Exit Condition 3: Return No action if max count of child orders created or parent order filled (all 3 child orders) */
        if (allChildOrders.size() >= MAX_CHILD_ORDERS || totalFilledQuantity >= parentOrderQuantity) {
            logger.info("[STRETCH-ALGO] Maximum number of orders: {} reached OR parent desired quantity has been filled {}. Returning No Action.", allChildOrders.size(), totalFilledQuantity);
            return NoAction.NoAction;
        }

        /* Determine the average of one instance of the OrderBook and then append to the list of averages */
        double bidAverage = mwaCalculator.calculateMovingWeightAverage(orderBookService.getBidLevels(state));
        double askAverage = mwaCalculator.calculateMovingWeightAverage(orderBookService.getAskLevels(state));
        bidAverages.add(bidAverage);
        askAverages.add(askAverage);

        /* Exit Condition 4: Return No action if we do not have sufficient data to calculate the overall trend of the  */
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

    /* Method 3: Evaluate trend based on the list of averages we have on the most recent 6 instances of historical data */
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
}