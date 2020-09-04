Solution attempts for the get1000 game
----------------------------------

[Get 1000 the game](http://www.bitbreeds.com/get1000)

Currently I have 4-5 attempts to create well playing AIs
for the game.

How to install and run:
```
mvn clean install
```
Should build things,

Then running the main methods under ```runner``` should pit some strategies against
each other. Files with relative paths are accessed when running, so ensure you run from the base directory.

## TODO
* Look into solving a subgame case by creating the full normal form representation, and finding the best equilibria
* MCTS seems to not play well at all, might be buggy.

## Strategy comparison
This is the main method of the project, which easily allows varying
strategies to be compared.

This will take one function for each player.

```
fun compareStrategies(st1 : (StateAndHist) -> Pos,
                      st2 : (StateAndHist) -> Pos)
```

That function must take a Game State and the history which reached that state
and return which postion you want place the current dice roll.

Then it runs ALL 6^9 games, and wins and draws are collected and logged.

## Strategies


### Expected value based AI
The first strategy tries to minimize the expected value of
the distance to a 1000, and does not care what the opponent might attempt.

Code for it is located under ```solver.expectedvalue```

### Monte-Carlo tree search based AI
Uses MCTS and random playout to create a strategy.

Code is located under ```solver.mcts```

_This does currently not work very well, so I am looking for a bug in 
the implementation_.

### Expectiminimax, tries to minimize the amounts of losses.
I have not yet been able to run this for the full game, so it is only 
tested in specfic game cases

Code is located under ```solver.minimax```

_This does also not work as well as expected, so looking for bugs_

### Backwards induction based strategies
These are strategies that start by solving the end states, then work their
way backwards, while taking the results of the end states into account.

Some of the methods are described [here](https://www.bitbreeds.com/Blog/solving-get1000-continued/), 
The use of _Subgameperfection_ as a term, is not completely correct. But the way these strategies
are calculated is inspired by that.

Solving for _Subgameperfection_ is sadly computationally out of reach
for me so far, since that means setting up some huge normal form matricies.
Something approaching that is my next plan for a solution.

Code is located under ```solver.backwardsinduction```

### Results
* So far the backwards induction based strategies have turned out to be the better ones.


### Log output
Log output is controlled by logback. Add of remove loggers
to get more/less debugging information


### Solution files
Some solutions are stored in various JSON based formats in the ```solutions``` folder.
Since some solutions take quite a bit of time to compute, these precomputed maps are great
for running comparisons.

The structure of these depend on the kind of solution, so each type of solution
may use different formats.

### Human games
Under ```game1000-solver/analysis``` are some games played by humans. Some day I will try and
see how humans play, compared to the different algorithmic approaches.

I need to filter games where people just pressed random crap though, seems there are some of those.