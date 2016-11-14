package ai;

import game.GameMap;
import game.Location;

public interface Scorer {
    double evaluate(GameMap gameMap, Location location);
}
