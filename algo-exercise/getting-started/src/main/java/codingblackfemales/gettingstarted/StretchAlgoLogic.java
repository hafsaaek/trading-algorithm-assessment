package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;

public class StretchAlgoLogic implements AlgoLogic {
     /*
        * Keep in mind:
        1. Best bid price will almost always be lower than best ask price 

        * Logic:
        1. Create an order using a stragey (vwap for exmaple)
        2. wait for it to be matched using market data 
            2a. figure out a way to actualy facilitate the magic - the same as what Addcancel has done
        3. if it isn't matched - then cancel 

    */

    @Override
    public Action evaluate(SimpleAlgoState state) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evaluate'");
    }

   
    
}
