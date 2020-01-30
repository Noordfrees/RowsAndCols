import java.awt.Point;
import java.util.*;

public class Player {
	public final String name;
	public final boolean ai;
	private long points;
	private final Brick[] bricks;
	
	public static final int kBricksPerPlayer = 6;
	
	public Player(String s, boolean a) {
		name = s;
		ai = a;
		points = 0;
		bricks = new Brick[kBricksPerPlayer];
	}
	public Brick[] getBricks() {
		return bricks;
	}
	public long getPoints() {
		return points;
	}
	public void addPoints(long delta) {
		points += delta;
	}
	
	public abstract class Move {
	}
	public class SwapMove extends Move {
		public final int[] bricks;
		public SwapMove(int ... indices) {
			bricks = indices;
		}
	}
	public class DefaultMove extends Move {
		public final Map<Point, Integer> bricks;
		public DefaultMove(Map<Point, Integer> b) {
			bricks = b;
		}
	}
	public Move aiStep(Game game) {
		/* TODO *
		 * Decide how to act:
		 * 1 – We iterate over all bricks and all free positions and check which bricks may be placed where.
		 * 2 – For each pair, we assume that we place the brick there and iterate again over all
		 *     now available positions and all remaining bricks, and so on recursively.
		 * 3 – After all recursive iterations we choose the pair with the highest number of points.
		 *     If several pairs are tied for best rating, we choose the one with fewest bricks.
		 *     If there´s still a tie, choose one at random.
		 * 4 – If no moves are possible at all, swap bricks.
		 *     Preferably keep bricks where all copies but this one are already on the board.
		 * TODO penalty for giving other players the chance to score many points
		 *      (e.g. placing the 5th brick in a row where someone else might be able to place the 6th brick)
		 */
		/* ArrayList<DefaultMove> allowedMoves = new ArrayList<>();
		
		final int[][] combs = allCombinations(bricks.length);
		for (int[] c : combs) {
			Map<Point, Brick> _board = game.getBoard();
			ArrayList<Point> _placed = new ArrayList<>();
			Map<Point, Integer> _moves = new HashMap<>();
			for (int i : c) {
				Map<Point, Brick> board = new HashMap<>(_board);
				ArrayList<Point> placed = new ArrayList<>(_placed);
				Map<Point, Integer> moves = new HashMap<>(_moves);
				for (Point p : pointsNear()) {
					if (!Game.mayPlaceAt(bricks[i], board, placed, p)) {
						break;
					}
					board.put(p, bricks[i]);
					placed.add(p);
					moves.put(p, i);
				}
			}
			allowedMoves.add(new DefaultMove(moves));
		} */
		
		
		return new SwapMove((int)(Math.random() * bricks.length));
	}
	private static Point[] pointsNear(Point[] pp) {
		Set<Point> r = new HashSet<>();
		for (Point p : pp) {
			boolean n = false;
			boolean s = false;
			boolean w = false;
			boolean e = false;
			for (Point ppp : pp) {
				if (ppp.x == p.x && ppp.y == p.y + 1) s = true;
				if (ppp.x == p.x + 1 && ppp.y == p.y) e = true;
				if (ppp.x == p.x && ppp.y == p.y - 1) n = true;
				if (ppp.x == p.x - 1 && ppp.y == p.y) w = true;
				if (n && s && w && e) break;
			}
			if (!n) r.add(new Point(p.x, p.y - 1));
			if (!w) r.add(new Point(p.x - 1, p.y));
			if (!s) r.add(new Point(p.x, p.y + 1));
			if (!e) r.add(new Point(p.x + 1, p.y));
		}
		return r.toArray(new Point[0]);
	}
	private static int[][] allCombinations(int n) {
		assert(n > 0);
		if (n == 1) {
			return new int[][] { new int[] { 1 }};
		}
		int[][] all = allCombinations(n - 1);
		final int x = all.length;
		int[][] result = new int[x * n][n];
		for (int r = 0; r < x; ++r) {
			for (int j = 0; j < n; ++j) {
				for (int k = 0; k < j; ++k) result[r * n + j][k] = all[r][k];
				result[r * n + j][j] = n;
				for (int k = j + 1; k < n; ++k) result[r * n + j][k] = all[r][k - 1];
			}
		}
		return result;
	}
}
