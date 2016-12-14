package tests;

import ai.Context;
import game.GameMap;
import game.Location;
import game.Site;
import junit.framework.TestCase;

public class PathfindingTests extends TestCase {
    public void testAStar() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Location goalLoc = new Location(3, 3);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;
        Context context = new Context(gameMap, null, 1, gameMap, null);
        context.aStar(originLoc, goalLoc);
    }

    public void testGoalIsNeighbor() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;

        Location goalLoc = new Location(0, 1);
        Context context = new Context(gameMap, null, 1, gameMap, null);
        Location step = context.aStar(originLoc, goalLoc);
        assertEquals(goalLoc, step);

    }

    public void testOriginIsGoal() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Location goalLoc = new Location(0, 0);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;
        Context context = new Context(gameMap, null, 1, gameMap, null);
        Location result = context.aStar(originLoc, goalLoc);
        assertEquals(goalLoc, result);
    }

}
