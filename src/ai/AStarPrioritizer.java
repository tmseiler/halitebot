package ai;

import game.GameMap;
import game.Location;
import game.Site;

import java.util.Comparator;

public class AStarPrioritizer implements Comparator<Site> {
    private GameMap gameMap;
    private Location goal;
    private int myID;

    public AStarPrioritizer(GameMap gameMap, Location goal, int myID) {
        this.gameMap = gameMap;
        this.goal = goal;
        this.myID = myID;
    }

    @Override
    public int compare(Site o1, Site o2) {
        double score1 = gameMap.getCost(o1, myID) + gameMap.getDistance(o1.location, goal);
        double score2 = gameMap.getCost(o2, myID) + gameMap.getDistance(o2.location, goal);
        if (score1 > score2) return 1;
        else if (score1 == score2) return 0;
        else return -1;
    }
}