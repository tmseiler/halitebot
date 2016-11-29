package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;
import game.Location;
import game.Site;


public class WaitQualifier extends Qualifier {
    public WaitQualifier(Context context, Action action) {
        super(context, action);

        double bestScore = 0.0;
        for (Location neighbor : context.getAgentNeighbors()) {
            Site site = context.gameMap.getSite(neighbor);
            double score;
            if(site.isFriendly) {
                score = context.acquisitionMap.getValue(neighbor);
                if(score > bestScore) {
                    bestScore = score;
                }
            }
        }

        final double finalBestScore = bestScore;
        addScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                if(finalBestScore > context.acquisitionMap.getValue(context.agentLocation)) {
                    return 0.0;
                } else {
                    return 1.0;
                }
            }
        });

        addWeightScorer(new BoundarynessScorer());
//        addScorer(new DiffusionClimberScorer());
    }

    public double getScore() {
        double scoreSum = 0.0;
        for (Scorer scorer : scorers) {
            scoreSum += scorer.score(context, action);
        }

        double weightedScore = scoreSum / scorers.size();
        for (Scorer weightedScorer : weightScorers) {
            weightedScore *= weightedScorer.score(context, action);
        }

        if (weightedScore > .25) {
            return weightedScore;
        } else {
            return 0.0;
        }
    }
}
