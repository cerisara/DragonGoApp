DragonGoApp
===========

Android app to play the game of go with the Dragon Go Server.

This is a new experimental android app to use for DGS, with the main objectives:

* Open-source; we welcome new contributors !
* Ad-free
* Off-line usage
* Minimize bandwidth usage: this is especially useful when you have a limited quota of 3G data connection; the code only transfers the minimum amount of textual data required with the server. There are only 2 buttons that create a connection with the server: "Get Games", which downloads the sgfs from the server, and "send", which uploads your move to the server.

For now, there are only a few most basic features implemented in DragonGoApp:

* login: secure local storage of your credentials, automatic login once you have entered them.
* status: look at the next games where it's your turn
* download the sgf of these games
* javascript goban based on eidogo to try some variations, and select the move you want to play
* upload your chosen move and switch to the next game.

Note that ending game, resign, comments are not supporting yet
All these will come soon.

That's all for now ! I expect much more features soon, but if you want more features in your android app, let me recommend you to (also) use the very good anDGS app that is available on the market.

