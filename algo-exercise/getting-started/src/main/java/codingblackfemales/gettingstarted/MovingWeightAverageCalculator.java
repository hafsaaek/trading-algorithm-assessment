package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.SimpleAlgoState;

import java.util.ArrayList;
import java.util.List;

public class MovingWeightAverageCalculator {
    private StretchAlgoLogic stretchAlgoLogic;

    private List<Double> bidAverages = new ArrayList<>();
    private List<Double> askAverages = new ArrayList<>();
    private SimpleAlgoState state;

    List<StretchAlgoLogic.OrderBookLevel> bidLevels = stretchAlgoLogic.getOrderBookLevels(state).get("Bid");
    List<StretchAlgoLogic.OrderBookLevel> askLevels = stretchAlgoLogic.getOrderBookLevels(state).get("Ask");

    public MovingWeightAverageCalculator(StretchAlgoLogic stretchAlgoLogic) {
        this.stretchAlgoLogic = stretchAlgoLogic;
    }

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

    public double calculateMovingWeightAverage(List<StretchAlgoLogic.OrderBookLevel> OrderBookLevel) {
        long totalQuantityAccumulated = 0;
        double weightedSum = 0;
        double weightedAverage;
        // Loop over OrderBooks instead of levels in 1 OrderBook! 6 averages are calculated in this method
        for (StretchAlgoLogic.OrderBookLevel order : OrderBookLevel) {
            totalQuantityAccumulated += order.quantity;
            weightedSum += (order.price * order.quantity);
        }
        weightedAverage = Math.round((weightedSum / totalQuantityAccumulated) * 100.0) / 100.0;
        return totalQuantityAccumulated > 0 ? weightedAverage : 0;
    }

    public void returnAveragesList(){
        double bidMovingWeightAverage = calculateMovingWeightAverage(bidLevels);
        double askMovingWeightAverage = calculateMovingWeightAverage(askLevels);

        bidAverages.add(bidMovingWeightAverage);
        askAverages.add(askMovingWeightAverage);
    }

}
