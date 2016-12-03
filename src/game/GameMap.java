package game;

import com.sun.tools.javac.code.Symbol;

import java.util.ArrayList;
public class GameMap{
    public ArrayList< ArrayList<Site> > contents;
    public int width;
    public int height;
    public static final int MAX_PRODUCTION = 16;
    public static final int MAX_SIZE = 50;
    public static final int MAX_DISTANCE = 2 * MAX_SIZE;
    public static final int MAX_STRENGTH = 255;
    public static final int NEUTRAL_OWNER = 0;

    public GameMap() {
        width = 0;
        height = 0;
        contents = new ArrayList< ArrayList<Site> >(0);
    }

    public GameMap(int width_, int height_) {
        width = width_;
        height = height_;
        contents = new ArrayList< ArrayList<Site> >(0);
        for(int y = 0; y < height; y++) {
            ArrayList<Site> row = new ArrayList<Site>();
            for(int x = 0; x < width; x++) {
                row.add(new Site());
            }
            contents.add(row);
        }
    }

    public boolean inBounds(Location loc) {
        return loc.x < width && loc.x >= 0 && loc.y < height && loc.y >= 0;
    }

    public double getDistance(Location loc1, Location loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);

        if(dx > width / 2.0) dx = width - dx;
        if(dy > height / 2.0) dy = height - dy;

        return dx + dy;
    }

    public double getAngle(Location loc1, Location loc2) {
        int dx = loc1.x - loc2.x;

        // Flip order because 0,0 is top left
        // and want atan2 to look as it would on the unit circle
        int dy = loc2.y - loc1.y;

        if(dx > width - dx) dx -= width;
        if(-dx > width + dx) dx += width;

        if(dy > height - dy) dy -= height;
        if(-dy > height + dy) dy += height;

        return Math.atan2(dy, dx);
    }

    public Location getLocation(Location loc, Direction dir) {
        Location l = new Location(loc);
        if(dir != Direction.STILL) {
            if(dir == Direction.NORTH) {
                if(l.y == 0) l.y = height - 1;
                else l.y--;
            }
            else if(dir == Direction.EAST) {
                if(l.x == width - 1) l.x = 0;
                else l.x++;
            }
            else if(dir == Direction.SOUTH) {
                if(l.y == height - 1) l.y = 0;
                else l.y++;
            }
            else if(dir == Direction.WEST) {
                if(l.x == 0) l.x = width - 1;
                else l.x--;
            }
        }
        return l;
    }

    public Location getLocation(Location loc, Direction d1, Direction d2) {
        return getLocation(getLocation(loc, d1), d2);
    }

    public ArrayList<Location> getNeighbors(Location loc) {
        ArrayList<Location> neighbors = new ArrayList<>(0);
        for (Direction d : Direction.CARDINALS)  {
            neighbors.add(getLocation(loc, d));
        }
        return neighbors;
    }

    public Site getSite(Location loc, Direction dir) {
        Location l = getLocation(loc, dir);
        return contents.get(l.y).get(l.x);
    }

    public Site getSite(Location loc) {
        return contents.get(loc.y).get(loc.x);
    }

    public void evaluateMove(int playerId, Move evaluatedMove) {
        if(evaluatedMove.dir == Direction.STILL)
            return;

        Site startingSite = getSite(evaluatedMove.loc);
        Site targetSite = getSite(evaluatedMove.loc, evaluatedMove.dir);
        if(targetSite.owner == playerId) {
            targetSite.strength = Math.max(targetSite.strength + startingSite.strength, MAX_STRENGTH);
        } else {
            if(startingSite.strength < targetSite.strength) {
                targetSite.strength = targetSite.strength - startingSite.strength;
            } else if(startingSite.strength > targetSite.strength) {
                targetSite.strength = startingSite.strength - targetSite.strength;
                targetSite.owner = playerId;
                targetSite.isFriendly = true;
                for (Location neighbor : getNeighbors(getLocation(evaluatedMove.loc, evaluatedMove.dir))) {
                    Site s = getSite(neighbor);
                    if((!s.isFriendly) && s.owner != GameMap.NEUTRAL_OWNER) {
                        s.strength = Math.max(0, s.strength - targetSite.strength);
                        if(s.strength == 0) {
                            s.owner = GameMap.NEUTRAL_OWNER;
                            s.isFriendly = false;
                        }
                    }
                }
            } else {
                targetSite.strength = 0;
            }
        }
        startingSite.strength = 0;
    }
}
