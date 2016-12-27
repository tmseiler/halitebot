package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static util.Logger.out;

public class GameMap {
    public ArrayList<ArrayList<Site>> contents;
    public ArrayList<ArrayList<HashMap<Integer, Piece>>> movedPieces;
    public ArrayList<ArrayList<Piece>> unmovedPieces;

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
        contents = new ArrayList<>(0);
    }

    public GameMap(int width_, int height_) {
        width = width_;
        height = height_;
        contents = new ArrayList<>(0);
        for (int y = 0; y < height; y++) {
            ArrayList<Site> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                Site site = new Site();
                site.location = new Location(x, y);
                row.add(site);
            }
            contents.add(row);
        }
    }

    public GameMap copy() {
        GameMap retMap = new GameMap(width, height);
        for (int y = 0; y < height; y++) {
            ArrayList<Site> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                Site s = new Site();
                Location loc = new Location(x, y);
                s.location = loc;
                Site oldSite = getSite(loc);
                s.strength = oldSite.strength;
                s.production = oldSite.production;
                s.owner = oldSite.owner;
                s.isFriendly = oldSite.isFriendly;
                row.add(s);
            }
            retMap.contents.set(y, row);
        }
        return retMap;
    }

    public boolean inBounds(Location loc) {
        return loc.x < width && loc.x >= 0 && loc.y < height && loc.y >= 0;
    }

    public double getDistance(Location loc1, Location loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);

        if (dx > width / 2.0) dx = width - dx;
        if (dy > height / 2.0) dy = height - dy;

        return dx + dy;
    }

    public Location getLocation(Location loc, Direction dir) {
        Location l = new Location(loc);
        if (dir != Direction.STILL) {
            if (dir == Direction.NORTH) {
                if (l.y == 0) l.y = height - 1;
                else l.y--;
            } else if (dir == Direction.EAST) {
                if (l.x == width - 1) l.x = 0;
                else l.x++;
            } else if (dir == Direction.SOUTH) {
                if (l.y == height - 1) l.y = 0;
                else l.y++;
            } else if (dir == Direction.WEST) {
                if (l.x == 0) l.x = width - 1;
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
        for (Direction d : Direction.CARDINALS) {
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

    private Piece getMovedPiece(Location loc, int owner) {
        return movedPieces.get(loc.y).get(loc.x).get(owner);
    }

    private void movePiece(Piece piece, Location loc) {
        Piece existingPiece = getMovedPiece(loc, piece.owner);
        if (existingPiece == null) {
            movedPieces.get(loc.y).get(loc.x).put(piece.owner, piece);
        } else {
            existingPiece.strength = Math.min(piece.strength + existingPiece.strength, MAX_STRENGTH);
        }
    }

    private Piece getUnmovedPiece(int y, int x) {
        return unmovedPieces.get(y).get(x);
    }

    private Piece getUnmovedPiece(Location loc) {
        return getUnmovedPiece(loc.y, loc.x);
    }

    public int simulateTurn(int myID, ArrayList<Move> moves) {
        int score = 0;
        int myCount = 0;
        int enemyCount = 0;
        movedPieces = new ArrayList<>();
        unmovedPieces = new ArrayList<>();
        // represent board as pieces
        for (int y = 0; y < height; y++) {
            ArrayList<HashMap<Integer, Piece>> unresolvedRow = new ArrayList<>();
            ArrayList<Piece> resolvedRow = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                unresolvedRow.add(new HashMap<>());
                Site s = getSite(new Location(x, y));
                if (s.owner == myID) myCount += s.strength;
                if (s.owner != myID && s.owner != NEUTRAL_OWNER) enemyCount += s.strength;
                resolvedRow.add(new Piece(s.owner, s.strength));
            }
            movedPieces.add(unresolvedRow);
            unmovedPieces.add(resolvedRow);
        }

        // move pieces and combine
        for (Move move : moves) {
            Location targetLoc = getLocation(move.loc, move.dir);
            if (move.dir != Direction.STILL) {
                Piece piece = getUnmovedPiece(move.loc);
                movePiece(new Piece(piece.owner, 0, true), move.loc);
                unmovedPieces.get(move.loc.y).set(move.loc.x, null);
                movePiece(piece, targetLoc);
            }
        }

        // add production for unmoved, non-neutral pieces
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // add production to str, move to moved pieces
                Piece unmovedPiece = getUnmovedPiece(y, x);
                if (unmovedPiece != null) {
                    Location loc = new Location(x, y);
                    if (unmovedPiece.owner != NEUTRAL_OWNER) {
                        unmovedPiece.strength = Math.min(unmovedPiece.strength + getSite(loc).production, MAX_STRENGTH);
                    }
                    movePiece(unmovedPiece, loc);
                }

            }
        }

        // apply damage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Location loc = new Location(x, y);
                for (int ownerId : movedPieces.get(y).get(x).keySet()) {
                    int damageDealt = 0;
                    Piece piece = getMovedPiece(loc, ownerId);
                    // for each direction
                    damageDealt += damagePieces(piece, loc, true);
                    if (ownerId != NEUTRAL_OWNER) {
                        for (Direction d : Direction.CARDINALS) {
                            // apply damage to non-neutral enemies
                            damageDealt += damagePieces(piece, getLocation(loc, d), false);
                        }
                    }
                    if (ownerId == myID) {
                        score += damageDealt;
                    }
                }
            }
        }

        // resolve state
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Location loc = new Location(x, y);
                for (Piece piece : getMovedPieces(loc).values()) {
                    piece.strength -= piece.damageTaken;
//                    if (piece.owner == myID) {
//                        score -= piece.damageTaken;
//                    }
                    Site site = getSite(loc);

                    if (piece.strength > 0) {
                        site.strength = piece.strength;
                        site.owner = piece.owner;
                        if (site.owner == myID) score += 5 * site.production;
                        break;
                    }

                    if (piece.damageTaken > 0) {
                        if (site.owner != myID) score += 3 * site.production;
                        site.owner = NEUTRAL_OWNER;
                    } else {
                        site.owner = piece.owner;
                    }
                    site.strength = 0;
                }
            }
        }
        return score;
    }

    public int simulateTurn(int myID, Move move) {
        ArrayList<Move> moves = new ArrayList<>();
        moves.add(move);
        return simulateTurn(myID, moves);
    }

    private int damagePieces(Piece piece, Location target, boolean damageNeutrals) {
        int damageDealt = 0;
        for (Piece enemyPiece : getMovedPieces(target).values()) {
            if (enemyPiece.owner != piece.owner) {
                if (enemyPiece.owner == NEUTRAL_OWNER && !damageNeutrals)
                    continue;
                enemyPiece.damageTaken += piece.strength;
                if (enemyPiece.owner != NEUTRAL_OWNER)
                    damageDealt += piece.strength;
            }
        }
        return damageDealt;
    }

    private HashMap<Integer, Piece> getMovedPieces(Location loc) {
        return movedPieces.get(loc.y).get(loc.x);
    }

    public int getCost(Site site, int ownerID) {
        if (site.strength > 200) {
            return 15;
        } else if (site.owner == ownerID) {
            return site.production > 4 ? 2 : 1;
        } else {
            if (site.strength > 0) return 10;
            else return 1;
        }
    }

    public void printMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Site s = getSite(new Location(x, y));
                out.printf("[%-2s %-4s]", s.owner, s.strength);
            }
            out.printf("\n");
        }
    }

    public ArrayList<Direction> moveToward(Location origin, Location destination) {
        ArrayList<Direction> possibleDirections = new ArrayList<>(0);

        int dx = origin.x - destination.x;
        int dy = origin.y - destination.y;

        double halfWidth = width / 2.0;
        if (dx != 0) {
            if ((Math.abs(dx) < halfWidth && dx < 0) || dx > halfWidth)
                possibleDirections.add(Direction.EAST);
            if (dx < halfWidth * -1.0 || Math.abs(dx) < halfWidth)
                possibleDirections.add(Direction.WEST);
        }


        double halfHeight = height / 2.0;
        if (dy != 0) {
            if ((Math.abs(dy) < halfHeight && dy < 0) || dy > halfHeight)
                possibleDirections.add(Direction.SOUTH);
            if (dy < halfHeight * -1.0 || Math.abs(dy) < halfHeight)
                possibleDirections.add(Direction.NORTH);
        }

        out.printf("moveToward %s to %s (%s)\n", origin, destination, possibleDirections);
        return possibleDirections;
    }

    public Direction anyMoveToward(Location origin, Location destination) {
        List<Direction> directions = moveToward(origin, destination).stream()
                .sorted((o1, o2) -> {
                    Location l1 = getLocation(origin, o1);
                    Location l2 = getLocation(origin, o2);
                    return Double.compare(getDistance(destination, l1), getDistance(destination, l2));
                })
                .collect(Collectors.toList());
        if (directions.size() > 0) return directions.get(0);
        else return Direction.STILL;
    }
}
