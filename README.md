# bidchamp

This project targets the [Everstor](http://evestor.co.uk/hackmanchester-2016) hack manchester challenge. 

## Principle

In a nutshell, this is a lottery / tombola / bingo application where people gamble on an item. But the originality is that 
the more money is put into the game, the more instances of the items are injected to the game as well. 

To make it simple : say you put a car, worth 10K into play. Players can bid any amount of money they want, up to 50% of the 
price of a car (ie, up to 5k). In the same fashion as kickstarter, the game last for a certain duration (say 24 hours), and 
people are only charged if at least the price of the car is pledged (for instance, if 5 people pledge 2k each). 

Obivously, the chances of a single player of winning the car decrease with the number of players who participate in the car. 
If originally, 5 players betting 2K had 20% chance each of winning the car, if 4 more players join the game for 2K each, 
it brings the total number of players to 9 with a 100/9 = 11.11% chance of winning. 

**HOWEVER**, say 10 people are participating, and each bet 2K. There is now 20K into play, which means we, 
as the game organizers, could put a second car into play. 10 players now gamble over 2 cars, meaning they are each back to 10% 
chance to win. 

**THAT IS WHY THIS LOTTERY IS INTERESTING** : at any point in time, the chance of winning of player has a lower limit, which
can only increase and is displayed to the player. And a player can actually see his chances increase by adding more money. 

## Additional incentives 

The organizer get a fixed percentage of any amount put into the game. In addition, a charity of our choice gets 
a fixed percentage of any amount put into the game, to which is added the possible remainder or the money : 
if a car costs 10K and 18K are collected for the game, the remainder is 8K. The money collected for the charity is 
displayed to everyone at the end of the game, which sweetens the defeat and make people think that their money went 
directly to a charity. 

Combine that with a social aspect, where you get to team up with your facebook friends to combine your chances, or invite
them if you deem they'd be interested to bet, and you get a good incentive to spend money irresponsibly. 


