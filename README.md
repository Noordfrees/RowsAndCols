# RowsAndCols
Tactical board game about placing bricks in various shapes and colours in rows. Try to place the 6th brick in a row to get bonus points - but don't give your competitors the chance to fill up your rows...

![RowsAndCols](https://repository-images.githubusercontent.com/237259241/c30cc600-438f-11ea-8b3b-e677f46c4530)

## Getting Started

No installation needed. Just clone or download the repository and run `javac *.java` in the base directory. You need to have a Java Development Kit, version 8 or later, installed in order to use `javac`. I recommend OpenJDK.

After compiling, you can start Mosaik from the base directory using `java Game`.

## The Game

Your aim is to gain as many points as possible. Points are awarded for placing bricks on the board.

Each brick has one of six colours (blue, red, green, yellow, cyan, and pink) and one of six shapes (circle, square, rhomb, flower, sun, and star). Each combination is present three times, making a total of 108 bricks in the game. One brick is placed in the middle of the board when the game starts.

The game is played by 2-6 players. Each player has 6 bricks displayed in the right column during his turn.

During his turn, a player has to take one of these two actions:

Click on one of your bricks to pick it up. (You can put it back again if you don't want to place it after all.) Click on the board to place the brick.

The rules for placing a brick are these:

- It has to be placed next to an existing brick.
- All bricks in a row have to be *either* all the same colour and all different shapes, *or* all the same shape and all different colours. The number of bricks a row can contain is therefore limited to 6.
- If you place several bricks in a turn, they all have to be placed in the same row, and at least one brick of this row had to be already present at the start of your turn.

It is allowed to connect two rows with a brick, provided that the resulting row follows these rules.

After your turn, click the button Next Player. Now you receive points for your placed bricks:

- For each row a brick is in, you get as many points as the row now has bricks. For example, if you place 2 bricks in a row that previously contained 3 bricks, it now has 5 bricks, so you get 5 points.
- This does not go for rows containing only one brick.
- When a brick is part of multiple rows, you receive points for each of these rows.
- Each row is counted only one per turn, even if you placed several bricks in it.
- If you managed to place the 6th brick in a row, you are awarded an additional 6 points for this row.

Your bricks are now refilled to 6 with random bricks drawn from the bag. And now it's the next player's turn.

### Swapping Bricks

At the beginning of your turn, you may also swap bricks instead of placing some if you do not want to place any of your bricks. Click the Swap button and then click on one or more of your bricks to put them back in the bag.

### The End Game

When the last brick is drawn from the bag, the end game begins. It is no longer allowed to swap bricks. You do not receive new bricks at the end of your turn. When a player places his last brick, he receives 6 bonus points at the end of his turn, and the game ends immediately. The player with the highest score wins, and he might even make it into the highscore list!

### The Top Bar

In the top left corner, you can see how many brick are left in the bag. Next to it are the buttons for ending your turn, undoing all moves made so far during this turn, entering brick swapping mode, and letting the AI move (from left to right). Inactive buttons are greyed out.

In the top-right corner, all players are listed with their scores. The current player is highlighted with bold font.

## The Menu

Press Escape to open the menu. The game will be saved automatically. (This is also the case when closing RowsAndCols during a game.)

In the menu, you can continue the saved game (if any), or start a new game with 2-6 players.

Use the Up/Down arrow keys to navigate the menu, and the Left/Right keys to change the value for the number of players. Use Enter to select the highlighted value. When a player is selected, Enter will toggle whether it is a computer player; use the keyboard to edit the player's name. Use Escape to quit.

On the right hand side, the highscores are displayed. Since players tend to get fewer points in games with many players, the actual score is defined as the product of the in-game score and the number of players in the game.

## Website

[Repository](https://github.com/Noordfrees/RowsAndCols)

[Bug reports go here](https://github.com/Noordfrees/RowsAndCols/issues)

## Credits

Created by [Benedikt Straub (@Noordfrees)](https://github.com/Noordfrees) in his spare time. I hope you like my work.

## License

GPL3. See the file [LICENSE](https://github.com/Noordfrees/RowsAndCols/blob/master/LICENSE) for details.
