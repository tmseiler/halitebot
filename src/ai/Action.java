package ai;

import game.GameMap;
import game.Location;
import game.Move;

public interface Action {
    double getScore();
    Move perform();
}
