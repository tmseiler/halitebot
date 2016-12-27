package ai;

import game.Location;

public class Mission {
    public Location origin;
    public Location target;
    public MovementQualifier qualifier;

    public Mission(Location origin, Location target, MovementQualifier qualifier) {
        this.origin = origin;
        this.target = target;
        this.qualifier = qualifier;
    }

    public boolean shouldMove() {
        return qualifier.shouldMove(origin);
    }
}