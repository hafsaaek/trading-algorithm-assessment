package codingblackfemales.gettingstarted;

import java.util.List;

public class MovingWeightAverageCalculator {

    /* Method to calculate the moving weight average for a given list of market orders */
    public double calculateMovingWeightAverage(List<OrderBookService.OrderBookLevel> OrderBookLevels){
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for (OrderBookService.OrderBookLevel order : OrderBookLevels) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round(((weightedSum / totalQuantityAccumulated) * 100)) / 100.0;
        return totalQuantityAccumulated > 0 ? weightedAverage : 0;
    }
}
