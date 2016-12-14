package ai;

import game.GameMap;
import game.Location;
import game.Site;

import java.util.Comparator;

class AStarPrioritizer implements Comparator<Site> {
    private GameMap gameMap;
    private Context context;
    private Location goal;

    public AStarPrioritizer(Context context, Location goal) {
        this.gameMap = context.gameMap;
        this.context = context;
        this.goal = goal;
    }

    @Override
    public int compare(Site o1, Site o2) {
        double score1 = context.getCost(o1) + gameMap.getDistance(o1.location, goal);
        double score2 = context.getCost(o2) + gameMap.getDistance(o2.location, goal);
        if (score1 > score2) return 1;
        else if (score1 == score2) return 0;
        else return -1;
    }
}