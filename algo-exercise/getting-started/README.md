# Hafsa's Trading Algorithm Documentation
## Table of Contents

1. [Project Objectives](#project-objectives)  
   1.1. [Basic Objective](#1-basic-objective)  
   1.2. [Stretch Objective](#2-stretch-objective)

2. [How to Get Started](#how-to-get-started)  
   2.1. [Pre-requisites](#pre-requisites)  
   2.2. [Opening the Project](#opening-the-project)  
   2.3. [Running Maven Tasks](#running-maven-tasks)

3. [Developing the Stretch Algo Logic: Agile Approach](#developing-the-stretch-algo-logic-agile-approach)  
   3.1. [Iteration 1: MyAlgoLogic](#iteration-1-myalgo-logic)  
   - [Test for Iteration 1](#test-for-iteration-1)  
   
   3.2. [Iteration 2: MyAlgoLogic2](#iteration-2-myalgo-logic2)  
   - [Test for Iteration 2](#test-for-iteration-2)  
   
   3.3. [Iteration 3: StretchAlgoLogic](#iteration-3-stretchalgologic)  
   - [Ensuring Clean, Modular Code using SOLID Principles](#ensuring-clean-modular-code-using-solid-principles)  
   - [Test for Iteration 3](#test-for-iteration-3)

## Project Objectives
#### 1. **Basic Objective**
Write a simple trading algorithm that creates and cancels child orders to meet specified conditions.
#### 2. **Stretch Objective**
Write an algo that can make money by buying shares when the order book is cheaper, and selling them when the order book is more expensive.

## How to Get Started
#### Pre-requisites
1. The project requires Java version 17 or higher

##### Note
This project is configured for Java 17. If you have a later version installed, it will compile and run successfully, but you may see warnings in the log like this, which you can safely ignore:

```sh
[WARNING] system modules path not set in conjunction with -source 17
```

#### Opening the project

1. Fork this repo in GitHub and clone it to your local machine
2. Open the project as a Maven project in your IDE (normally by opening the top level pom.xml file)
3. Click to expand the "getting-started" module
4. Navigate to the [StretchAlgoBackTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/StretchAlgoBackTest.java), [StretchAlgoTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/StretchAlgoTest.java) and [StretchAlgoLogic.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/main/java/codingblackfemales/gettingstarted/StretchAlgoLogic.java)
5. You're ready to go!


##### Note
You will first need to run the Maven `install` task to make sure the binary encoders and decoders are installed and available for use. You can use the provided Maven wrapper or an installed instance of Maven, either in the command line or from the IDE integration.

To get started, run the following command from the project root: `./mvnw clean install`. Once you've done this, you can compile or test specific projects using the `--projects` flag, e.g.:

- Clean all projects: `./mvnw clean`
- Test all `algo-exercise` projects: `./mvnw test --projects algo-exercise`
- Compile the `getting-started` project only: `./mvnw compile --projects algo-exercise/getting-started`

## Developing the Stretch Algo Logic: Agile Approach

The stretch algo logic that aims to create a profit was developed through a number of iterations using an agile development methodology 

### Iteration 1: [MyAlgoLogic.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/main/java/codingblackfemales/gettingstarted/MyAlgoLogic.java)
**Goal**: Create a basic algorithm that Creates & Cancels child orders by:
1. Maintain 3 passive child orders on the market, each 100 shares, to fill a parent market order of 300 shares. 
2. Create new child orders if there are fewer than 3 orders on the market. 
3. Cancel the oldest active order when there are 3 or more active orders. 
4. Return NO Action if:
   1. The total filled quantity reaches 300 shares. 
   2. More than 4 child orders (active + canceled) have already been created. 
   3. 3 (fully executed) child orders have fully filled.

#### Over-Execution has been accounted for by ensuring:
* The total filled quantity of all orders does not exceed the parent order quantity  
* If 3 orders have been fully filled, no more orders are created.

#### Test for iteration 1:
1.  [MyAlgoBackTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/MyAlgoBackTest.java)
2. [MyAlgoTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/MyAlgoTest.java)



### Iteration 2: [MyAlgoLogic2.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/main/java/codingblackfemales/gettingstarted/MyAlgoLogic2.java)
**Goal**: Create a basic algorithm that Creates & Cancels child orders depending on the Market Status. This was implemented using the following logic:
1. Maintain 3 active child orders on the market, each for 100 shares, to fill a parent order of 300 shares. 
2. Cancel an order if it is active but unfilled and the stock market is closed (time based on LSEG opening times)
3. Stop the program (i.e. return No Action) when either of the below are true:
   1. The total filled quantity reaches the parent order
   2. Three child orders have fully filled to prevent over-execution. 
   3. Three child orders (active + canceled) have been created.
   None of the above conditions or steps 1 & 2 are true.

#### Test for iteration 2:
1.  [MyAlgo2BackTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/MyAlgo2BackTest.java)
2. [MyAlgo2Test.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/MyAlgo2Test.java)


### Iteration 3: [StretchAlgoLogic.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/main/java/codingblackfemales/gettingstarted/StretchAlgoLogic.java)
**Goal**: Create a profitable algorithm building on the last algorithm developed in the second iteration

This algorithm builds on the basic algorithm logic by adding orders on the BUY side when the market favors buying low (sellers are placing lower ask offers than historic data) or when the market favors selling at a higher price (ask prices are going back up).

#### The market trend is determined using the Moving Weight Average (MWA) strategy:

1. An average of each instance of the order book is calculated over 6 occurrences to ensure an accurate trend of the market is captured. 
2. The sum of those 6 averages is calculated. 
3. If the ask side trend demonstrates a strong decline, the algorithm will BUY to secure a security at a cheaper price. 
4. If the bid side shows an increasing trend and the ask side shows an increasing trend, the algorithm will SELL those previously acquired shares at a higher price.

#### Assumptions
The algorithm is provided with a market order to BUY 300 shares or SELL 300 shares.
Orders that are not filled are cancelled by the end of the day or if the market is closed (public holidays have not been accounted for).

#### Ensuring Clean, Modular Code through the use of SOLID principles
The logic has been designed to implement SOLID principles, such as Loose Coupled Code and Single Responsibility Principle. The following responsibilities have been separated:

1. **MarketStatus**: Determines if the market is OPEN or CLOSED for cancellation logic.
2. **OrderBookService**: Retrieves bid and ask orders in a list to evaluate the market trend for each side.
3. **MovingWeightAverageCalculator**: Calculates the moving weight average of a given list of market orders.
4. **StretchAlgoLogic**: Determines the trend using a list of moving weight averages for each side of the OrderBook, creates new orders (BUY/SELL), cancels orders after the market is closed, and returns the appropriate action.

#### Test for iteration 3:
1. [StretchAlgoBackTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/StretchAlgoBackTest.java)
2. [StretchAlgoTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/orderbook-fix/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/StretchAlgoTest.java)
3. [MovingWeightAverageCalculatorTest.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/MovingWeightAverageCalculatorTest.java)
4. [SimpleMarketStatus.java](https://github.com/hafsaaek/trading-algorithm-assessment/blob/main/algo-exercise/getting-started/src/test/java/codingblackfemales/gettingstarted/SimpleMarketStatusTest.java)