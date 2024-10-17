package codingblackfemales.gettingstarted;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MovingWeightAverageCalculatorTest {
    MovingWeightAverageCalculator mwaCalculator = new MovingWeightAverageCalculator();

    @Test
    public void testMovingWeightAverageCalculatorMethod(){
        OrderBookService.OrderBookLevel order1 = new OrderBookService.OrderBookLevel(100, 100);
        OrderBookService.OrderBookLevel order2 = new OrderBookService.OrderBookLevel(90, 200);
        OrderBookService.OrderBookLevel order3 = new OrderBookService.OrderBookLevel(80, 300);
        List<OrderBookService.OrderBookLevel> orderArray = new ArrayList<>(Arrays.asList(order1, order2, order3));        double movingWeightAverage = mwaCalculator.calculateMovingWeightAverage(orderArray);
        assertEquals(86.67, movingWeightAverage, 0.01);
    }
}
