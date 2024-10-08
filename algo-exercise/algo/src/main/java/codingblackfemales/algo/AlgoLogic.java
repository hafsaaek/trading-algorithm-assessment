package codingblackfemales.algo;

import codingblackfemales.action.Action;
import codingblackfemales.sotw.SimpleAlgoState;

public interface AlgoLogic {
    Action evaluate(final SimpleAlgoState state); // evaluates action based on a given state 
}
