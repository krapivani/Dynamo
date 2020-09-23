# Dynamo

This is Dynamo-style key-value storage project; this project implements a simplified version of Dynamo. There are three main pieces  implemented: 1) Partitioning, 2) Replication, and 3) Failure handling.

The main goal is to provide both availability and linearizability at the same time. In other words, the dynamo always perform read and write operations successfully even under failures. At the same time, a read operation should always return the most recent value. 

The dynamo has five nodes which fail at times but stores information despite failures. Project design idea from CSE486 course [UB - Steve Ko].
