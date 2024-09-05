package codingblackfemales.util;

import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;

public class Util {

    // For a given string 's' of length x , add enough spaces till the string length = 'n' %- = to the right vice versa 
    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }

    public static String orderBookToString(final SimpleAlgoState state){

        final StringBuilder builder = new StringBuilder();

        int maxLevels = Math.max(state.getAskLevels(), state.getBidLevels()); // find the max number of rows we need for the order book 

        builder.append(padLeft("|----BID-----", 12) + "|" + padLeft("|----ASK----", 12) + "|" + "\n"); // add BID & ASK to the string 

        // 
        for(int i=0; i<maxLevels; i++){

            if(state.getBidLevels() > i){
                BidLevel level = state.getBidAt(i);
                builder.append(padLeft(level.quantity + " @ " + level.price, 12));
            }else{
                builder.append(padLeft(" - ", 12) + ""); // placeholder if no bid found
            }

            if(state.getAskLevels() > i){
                AskLevel level = state.getAskAt(i);
                builder.append(padLeft(level.quantity + " @ " + level.price, 12));
            }else{
                builder.append(padLeft(" - ", 12) + ""); // placeholder if no ask found
            }

            builder.append("\n");
        }

        builder.append(padLeft("|------------", 12) + "|" + padLeft("|-----------", 12) + "|" + "\n"); // add border to finalise building the orderbook 

        return builder.toString();
    }
}
