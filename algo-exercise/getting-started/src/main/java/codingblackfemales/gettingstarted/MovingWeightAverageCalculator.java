package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.SimpleAlgoState;

import java.util.ArrayList;
import java.util.List;

public class MovingWeightAverageCalculator {
    private OrderBookService orderBookService;
    private SimpleAlgoState state;

    public MovingWeightAverageCalculator(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    public double calculateMovingWeightAverage(List<OrderBookService.OrderBookLevel> OrderBookLevel){
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for (OrderBookService.OrderBookLevel order : OrderBookLevel) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round(((weightedSum / totalQuantityAccumulated) * 100)) / 100.0;
        return totalQuantityAccumulated > 0 ? weightedAverage : 0;
    }


}
