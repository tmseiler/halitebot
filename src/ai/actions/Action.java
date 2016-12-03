package ai.actions;

import game.Direction;
import game.Location;
import game.Move;
import game.Site;

public interface Action {
    Direction getStepDirection();
    Location getStep();
    Location getTarget();
    Site getTargetSite();
    Move perform();

    Site getStepSite();
}
