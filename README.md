# Coin Trader Bot

## Description
Coin Trader Bot is a framework for implementing trading strategies and running them against stock and cryptocurrency exchanges. It allows for concurrently monitoring various exchanges, executing buy or sell orders based on strategies, and performing testing on historical data. It can use either the CPU or GPU (OpenCL or CuBlas) for computations. It also includes a basic neural network implementation for strategies using machine learning. The project was built mostly from scratch, using only APIs provided by Java (except for OpenCL and CuBlas for GPU computations and gson for JSON parsing). Because of this, it includes a custom logger, config file parser, HTTP Client, and Math library (mostly linear alegbra).

## Implemented Exchanges
[Bittrex](https://bittrex.com/) and [Poloniex](https://poloniex.com/) are currently implemented.

## Implemented Algorithms
A simple [Bollinger Band](https://www.investopedia.com/terms/b/bollingerbands.asp) strategy is currently implemented.
