package tests;

import game.*;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;

public class GameMapTests extends TestCase {
    int ENEMY_ID = 2;

    @Test
    public void testStill() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(0, 0);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 100;
        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.STILL, 1));
        gameMap.simulateTurn(1, moves);
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
        gameMap.simulateTurn(1, moves);
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
        gameMap.simulateTurn(1, moves);
        assertEquals(50, gameMap.getSite(targetLoc).strength);
        assertEquals(1, gameMap.getSite(targetLoc).owner);

        assertEquals(0, gameMap.getSite(originLoc).strength);
        assertEquals(1, gameMap.getSite(originLoc).owner);
    }

    public void testOverkill() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(1, 1);
        Location targetLoc = gameMap.getLocation(originLoc, Direction.EAST);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 200;

        gameMap.getSite(targetLoc, Direction.NORTH).strength = 50;
        gameMap.getSite(targetLoc, Direction.NORTH).owner = ENEMY_ID;
        gameMap.getSite(targetLoc, Direction.EAST).strength = 50;
        gameMap.getSite(targetLoc, Direction.EAST).owner = ENEMY_ID;
        gameMap.getSite(targetLoc, Direction.SOUTH).strength = 50;
        gameMap.getSite(targetLoc, Direction.SOUTH).owner = ENEMY_ID;

        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.EAST, 1));
        gameMap.simulateTurn(1, moves);
        assertEquals(gameMap.getSite(targetLoc).owner, 1);
        assertEquals(gameMap.getSite(targetLoc).strength, 50);
        assertEquals(gameMap.getSite(targetLoc, Direction.NORTH).owner, GameMap.NEUTRAL_OWNER);
        assertEquals(gameMap.getSite(targetLoc, Direction.EAST).owner, GameMap.NEUTRAL_OWNER);
        assertEquals(gameMap.getSite(targetLoc, Direction.SOUTH).owner, GameMap.NEUTRAL_OWNER);
        assertEquals(gameMap.getSite(targetLoc, Direction.NORTH).strength, 0);
        assertEquals(gameMap.getSite(targetLoc, Direction.EAST).strength, 0);
        assertEquals(gameMap.getSite(targetLoc, Direction.SOUTH).strength, 0);
    }

    public void testEnemyUnitsCollideOnNeutral() {
        GameMap gameMap = new GameMap(5, 5);
        Location originLoc = new Location(1, 1);
        Location neutralLoc = gameMap.getLocation(originLoc, Direction.EAST);
        Location enemyOriginLoc = gameMap.getLocation(originLoc, Direction.EAST, Direction.EAST);
        Site s = gameMap.getSite(originLoc);
        s.owner = 1;
        s.strength = 200;

        Site enemySite = gameMap.getSite(enemyOriginLoc);
        enemySite.owner = ENEMY_ID;
        enemySite.strength = 150;

        Site neutralSite = gameMap.getSite(neutralLoc);
        neutralSite.owner = GameMap.NEUTRAL_OWNER;
        neutralSite.strength = 25;

        ArrayList<Move> moves = new ArrayList<>();
        moves.add(new Move(originLoc, Direction.EAST, 1));
        moves.add(new Move(enemyOriginLoc, Direction.WEST, ENEMY_ID));
        gameMap.simulateTurn(1, moves);
        assertEquals(25, gameMap.getSite(neutralLoc).strength);
        assertEquals(1, gameMap.getSite(neutralLoc).owner);
    }
}
