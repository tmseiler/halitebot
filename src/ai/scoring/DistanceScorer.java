package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.Location;

public class DistanceScorer implements Scorer {
    private Location subjectLocation;

    public DistanceScorer(Location subjectLocation) {
        this.subjectLocation = subjectLocation;
    }

    @Override
    public double score(Context context, Action action) {
        double distance = context.gameMap.getDistance(action.getStep(), subjectLocation);
        return 1 - (distance / ((double) context.gameMap.height * (double) context.gameMap.width));
    }
}
