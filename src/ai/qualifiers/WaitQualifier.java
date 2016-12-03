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
        Location bestLoc = null;
        for (Location neighbor : context.getAgentNeighbors()) {
            Site site = context.gameMap.getSite(neighbor);
            double score;
            if(!site.isFriendly) {
                score = context.expansionMap.getValue(neighbor);
                if(score > bestScore) {
                    bestScore = score;
                    bestLoc = neighbor;
                }
            }
        }

        final double finalBestScore = bestScore;
        final Location finalBestLoc = bestLoc;
        addScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                if(finalBestScore > context.expansionMap.getValue(context.agentLocation) &&
                        context.gameMap.getSite(context.agentLocation).strength > context.gameMap.getSite(finalBestLoc).strength) {
                    return 0.0;
                } else {
                    return 1.0;
                }
            }
        });

//        addScorer(new DiffusionClimberScorer());
    }

//    public double getScore() {
//        double scoreSum = 0.0;
//        for (Scorer scorer : scorers) {
//            scoreSum += scorer.score(context, action);
//        }
//
//        double weightedScore = scoreSum / scorers.size();
//        for (Scorer weightedScorer : weightScorers) {
//            weightedScore *= weightedScorer.score(context, action);
//        }
//
//        if (weightedScore > .95) {
//            return weightedScore;
//        } else {
//            return 0.0;
//        }
//    }
}
