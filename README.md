DragonGoApp
===========

Android app to play the game of go with the Dragon Go Server.

This is a new experimental android app to use for DGS, with the main objectives:

* Open-source; we welcome new contributors !
* Ad-free
* Minimize bandwidth usage: this is especially useful when you have a limited quota of 3G data connection; the code only transfers the minimum amount of textual data required with the server. There are only 2 buttons that create a connection with the server: "Get Games", which downloads the sgfs from the server, and "send", which uploads your move to the server.
* Off-line usage (still to be implemented / improved)

It's still at the alpha stage of development, but you can already find the following features:

* login: secure local storage of your credentials, automatic login once you have entered them.
* status: look at the next games where it's your turn
* download the sgf of these games
* javascript goban based on eidogo to try some variations, and select the move you want to play
* upload your chosen move, or send a pass move, or resign, or skip this game
* automatic detection of scoring phase after two passes; then mouse cliks = mark dead stones
* unread messages are downloaded and shown; you can then mark them as read or no.
* support for receiving games invitations: you can accept, decline or skip.
* automatically goes to the last move played after downloading a game
* nice support of network errors with "short" timeout and no crash

Thanks to the integrated eidogo editor, the following features are also supported:

* SGF editor: navigate in the game, study variations
* View game comments
* View game information (opponents names and ranks, komi...)
* View move number

Features that will be supported soon:

* Endgame: score checking, choosing dead stones (nearly done, to come very soon !)
* Sending comments

Here is a quickstart guide:

* First open the menu, click on settings and enter your DGS login and password (it is securely stored !)
* Then click on "GetGames" and wait a few seconds: it will report after maximum 6 seconds whether it has been able to
succesfully connect or not, and if so, it will report the number of games where it's your turn.
"GetGames" will also check for unread messages and game invitations and display them if there are any.
You then have the opportunity to mark messages as unread or not, invite or declide or skip game invitations.
* Then wait 2 seconds for the first game to show up
* You can navigate the game, explore variations, nothing is sent to the server as long as you don't press the SEND button !
* The comments associated with each move are shown in the bottom text area. Game information is stored in the comments associated with the game start.
* When you press the SEND button, the single last move that is visible on screen is sent (this is the stone marked with a
red square, or the pass move if no stone is marked)
* There are actually two ways to send a pass move: either pressing the top "PASS" button, or in the board, recording a pass move with the board pass button and pressing the board send button.
* If you don't want to play right now this game, you can skip this game by pressing the top-right "..." button, which shows up a new column of buttons; you can choose in this column the "skip this game" button; the next game will show up.
* When you have gone through all the games, either by playing one move or skipping a game, a message "no more game to show" is displayed. You can then press again the "GetGames" button to see if you have new (or skipped) games to play.


That's all for now, but I expect much more features soon...

