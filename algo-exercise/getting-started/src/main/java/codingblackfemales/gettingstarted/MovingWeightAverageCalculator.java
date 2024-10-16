package codingblackfemales.gettingstarted;

import java.util.List;

/**
 * There may be cases for this to be modeled as an interface if we expect the strategy to be configurable.
 */
public class MovingWeightAverageCalculator {

    public double calculateMovingWeightAverage(List<OrderBookService.OrderBookLevel> orderBookLevels) {
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for (OrderBookService.OrderBookLevel order : orderBookLevels) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round(((weightedSum / totalQuantityAccumulated) * 100)) / 100.0;
        return totalQuantityAccumulated > 0 ? weightedAverage : 0;
    }
}
