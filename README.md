# Kokomo
Kokomo is a competitor robot built for Robocode that uses regression tree based machine learning (called [Dynamic Segmentation](https://robowiki.net/wiki/Wiki_Targeting/Dynamic_Segmentation) by some in the Robocode community) to learn its opponent's movement strategy.

Like most top competitor robots, Kokomo collects [GuessFactors](https://robowiki.net/wiki/GuessFactor) using Waves - both its own and its opponents. Movement uses traditional [Wave Surfing](https://robowiki.net/wiki/Wave_Surfing) (a slight improvement over [BasicGFSurfer](https://robowiki.net/wiki/Wave_Surfing_Tutorial)'s reference Wave Surfing implementation). Targeting uses a custom [incremental](https://en.wikipedia.org/wiki/Incremental_learning) regression tree.

## Future Plans
* Improve the performance of the regression tree and solve several memory issues that occur when matches go on too long
* Implement serialization, like described in the Wiki Targeting page, to improve early round performance against top bots
* Expand regression tree into a forest, starting with an implementation of a k-d tree and a k-nn targeting algorithm
* Investigate using machine learning for movement as well
