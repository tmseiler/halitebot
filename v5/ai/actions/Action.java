package ai.actions;

import game.Direction;
import game.Location;
import game.Move;
import game.Site;

public interface Action {
    Direction getTargetDirection();
    Location getTarget();
    Site getTargetSite();
    Move perform();
}
