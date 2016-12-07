package tests;

import game.*;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;

public class GameMapTests extends TestCase {
    @Test
    public void testStill() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;
        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.STILL, 1));
        gameMap.performMoves(moves);
        assertEquals(100, gameMap.getSite(originLoc).strength);
        assertEquals(1, gameMap.getSite(originLoc).owner);
    }

    public void testSimpleMovement() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;
        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.EAST, 1));
        gameMap.performMoves(moves);
        assertEquals(100, gameMap.getSite(originLoc, Direction.EAST).strength);
        assertEquals(1, gameMap.getSite(originLoc, Direction.EAST).owner);

        assertEquals(0, gameMap.getSite(originLoc).strength);
        assertEquals(1, gameMap.getSite(originLoc).owner);
    }

    public void testCaptureNeutral() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Location targetLoc = gameMap.getLocation(originLoc, Direction.EAST);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;

        Site neutral = gameMap.getSite(targetLoc);
        neutral.owner = 0;
        neutral.strength = 50;

        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.EAST, 1));
        gameMap.performMoves(moves);
        assertEquals(50, gameMap.getSite(targetLoc).strength);
        assertEquals(1, gameMap.getSite(targetLoc).owner);

        assertEquals(0, gameMap.getSite(originLoc).strength);
        assertEquals(1, gameMap.getSite(originLoc).owner);
    }
}
