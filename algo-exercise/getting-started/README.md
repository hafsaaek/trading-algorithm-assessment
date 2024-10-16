
# Welcome to Trading Algorithm Assessment (CBF x UBS)

## Table of Contents
1. [Project Objectives](#project-objectives)
2. [Progression of Logic](#progression-of-logic)
   - [Basic Logic: Market Order for 300 Shares](#1-basic-logic-buy-300-shares-at-market-price)
   - [Second Logic](#2-second-logic)
   - [Final Logic](#3-final-logic)
3. [UI Exercise](#ui-considerations)

## Project Objectives
### 1. **Basic Objective**
Write a simple trading algorithm that creates and cancels child orders to meet specified conditions.
### 2. **Stretch Objective**
Write an algo that can make money by buying shares when the order book is cheaper, and selling them when the order book is more expensive.

## Basic Objective 

### Basic Trading Logic 1: Create 3 child orders to fill a parent market order of buying 300 shares 

To achieve the basic objective, the algorithm is designed to fill a parent market order for 300 shares by managing child orders as follows:
1. **Order Management**: Maintain 3 passive child orders of 100 shares each to cumulatively fill the parent market order of 300 shares.
2. **Order Creation**: If there are fewer than 3 active child orders, the algorithm will create new orders to maintain this threshold.
3. **Order Cancellation**: When there are 3 or more active child orders, the oldest active order will be canceled.

4. **No Further Action**: Return No action if:
   - The total filled quantity reaches 300 shares to prevent over-execution
   - More than 4 child orders (active + canceled) have been created 
   - Three child orders have been fully executed 


### Basic Logic 2: Create 3 "Time in force" DAY orders to fill a parent market order of buying 300 shares

To achieve this alternative basic objective, the algorithm is designed to fill a parent market order for 300 shares by managing child orders as before but cancelling the orders if the market closes. 

1. **Order Management**: Maintain 3 passive child orders of 100 shares each to cumulatively fill the parent market order of 300 shares.

2. **Order Creation**: If there are fewer than 3 active child orders, the algorithm will create new orders to maintain this threshold.

3. **Order Cancellation**: Cancel existing orders if the market is closed by using the isMarketClosed() returns true

4. **No Further Action**: Return No action if:
   * The stock market is closed (Before 8am, after 4.35pm or on weekends)
   * The total filled quantity reaches 300 shares to prevent over-execution
   - More than 3 child orders (active + canceled) have been created
   - Three child orders have been fully executed


---

## Stretch Objective

### LOGIC BEHIND THE STRETCH OBJECTIVE OUTCOME
*  This Algo logic builds on the basic algo logic by
    * --> Adding orders on the BUY side when favours buying low (sellers are placing lower ask offers than historic data) OR when the market favours selling at a higher price (ask price are going back up), place a SELL order that purchases those 300 shares previously bought at a higher price or vice versa to ensure a profit can be made.
* The Market trend is determined using the Moving Weight Average strategy by:
    * 1. An average of each instance of the order book is calculated over 6 occurrences to ensure an accurate trend of the market is captured
    * 2. The sum of those 6 averages are calculated and then added up
    * 3. If the ask side trend demonstrates a strong decline --> BUY to secure a security at a cheaper price
    * 4. If the bid side shows an increasing trend and the ask side shows an increasing trend -->  SELL those previously acquired shares at a higher price
* Assumptions for this logic:
    * We are either provided with a market order to BUY 300 shares or SELL 300 shares by sending 3 child orders
    * If the trend favours BUYING cheap, SELL high for later or vice versa
    * Orders that are not filled are cancelled by end of Day OR if the market is closed [Public holidays have not been accounted for] - no orders are placed *
* The logic has been ensured to implement SOLID principles such as Loose coupled code and Single responsibility principle: By separating the functionalities the logic class should not be responsible including:
  * 1. MarketStatus: Determines if the market is OPEN or Closed for cancellation logic
  * 2. OrderBookService: Retrieves bid and ask orders in a list to evaluate market trend for each side
  * 3. MovingWeightAverageCalculator: Calculates
  * 4. Stretch Algo Logic: With the injected  MarketStatus, OrderBookService & MovingWeightAverageCalculator dependencies, this class determines which ACTION should be returned e.g.,: Determine the trend using a list of moving weight averages for each side of the OrderBook, Create new orders (BUY/SELL), Cancel orders after market is closed, return no action if market is closed when evaluate method is called or market is stable or full parent order is filled or max number of child orders (3) are created.
*  */


