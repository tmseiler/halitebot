package ai;

import game.*;

public class WaitAction implements Action {

    private GameMap gameMap;
    private Location friendlyLoc;

    public WaitAction(GameMap gameMap, Location friendlyLoc) {
        this.gameMap = gameMap;

        this.friendlyLoc = friendlyLoc;
    }

    @Override
    public String toString() {
        return "WaitAction{" +
                "friendlyLoc=" + friendlyLoc +
                '}';
    }

    @Override
    public double getScore() {
        double score = 1.0;
        Site site = gameMap.getSite(friendlyLoc);
        if(site.strength == 0) score += 100;
        if(site.strength < (site.production * 3)) score += 25;
        return score;
    }

    @Override
    public Move perform() {
        return new Move(friendlyLoc, Direction.STILL);
    }
}
