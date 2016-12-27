package ai;

import game.Location;

public interface MovementQualifier {
    boolean shouldMove(Location loc);
}
