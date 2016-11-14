package ai;

import game.*;

public class BolsterAction implements Action {


    private final GameMap gameMap;
    private final Location friendlyLoc;
    private final Direction direction;

    public BolsterAction(GameMap gameMap, Location friendlyLoc, Direction direction) {
        this.gameMap = gameMap;
        this.friendlyLoc = friendlyLoc;
        this.direction = direction;
    }

    @Override
    public double getScore() {
        double score = 0.0;
        Site site = gameMap.getSite(friendlyLoc);
        Site neighbor = gameMap.getSite(friendlyLoc, direction);
        if(site.strength == 0) score -= 100.00;
        if(site.strength < neighbor.strength) score += 25;
        if(site.strength > site.production * 3) score += 25;
        return score;
    }

    @Override
    public Move perform() {
        return new Move(friendlyLoc, direction);
    }
}
