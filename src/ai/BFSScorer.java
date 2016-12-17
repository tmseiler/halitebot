package ai;

import game.Location;

public interface BFSScorer {
    double evaluatePosition(Location loc);
}
