import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Game {
	
	private final JFrame frame;
	private final JLabel display;
	
	private final Map<Point, Brick> board;
	private final ArrayList<Brick> remainingBricks;
	
	private Player[] players;
	private int currentPlayer;
	private boolean gameOver, gameNewlyStarted;
	private final ArrayList<Point> bricksPlaced;
	private ArrayList<Brick> swappingBricks;
	private final Rectangle[] brickPositions;
	private Brick currentBrick;
	private Point currentBrickPosition;
	private Rectangle buttonSwap, buttonNext, buttonAI, buttonUndo;
	
	private static class Menu {
		public static class HighscoreEntry {
			public final String name;
			public final long points;
			public final int nrPlayers;
			public HighscoreEntry(String n, long p, int i) {
				name = n;
				points = p;
				nrPlayers = i;
			}
		}
		private static HighscoreEntry[] loadHighscores() {
			HighscoreEntry[] h;
			try {
				java.util.List<String> list = Files.readAllLines(kHighscores.toPath());
				h = new HighscoreEntry[list.size()];
				int index = 0;
				for (String ss : list) {
					final String[] data = ss.split(",");
					final long p = Long.valueOf(data[0]);
					final int nr = Integer.valueOf(data[1]);
					String str = data[2];
					for (int i = 3; i < data.length; ++i) str += "," + data[i];
					h[index++] = new HighscoreEntry(str, p, nr);
				}
			} catch (Exception x) {
				h = new HighscoreEntry[10];
				for (int i = 0; i < h.length; ++i) h[i] = new HighscoreEntry("Nobody", 0, 0);
			}
			return h;
		}
		private static final HighscoreEntry[] highscores = loadHighscores();
		public void registerHighscore(HighscoreEntry e) {
			highlightHighscore = -1;
			for (int i = 0; i < highscores.length; ++i) {
				if (e.points * e.nrPlayers > highscores[i].points * highscores[i].nrPlayers) {
					highlightHighscore = i;
					break;
				}
			}
			if (highlightHighscore < 0) return;
			for (int i = highscores.length - 1; i > highlightHighscore; --i) highscores[i] = highscores[i - 1];
			highscores[highlightHighscore] = e;
			try {
				PrintWriter w = new PrintWriter(kHighscores);
				for (int i = 0; i < highscores.length; ++i) {
					if (i > 0) w.println();
					w.print(highscores[i].points + "," + highscores[i].nrPlayers + "," + highscores[i].name);
				}
				w.close();
			} catch (Exception x) {
				System.out.println("ERROR: Could not save highscores: " + x);
			}
		}
		
		private static class PlayerSetting {
			public String name;
			public boolean ai;
			public PlayerSetting(String s, boolean a) {
				name = s;
				ai = a;
			}
		}
		
		public void draw(Graphics2D g) {
			final Dimension d = game.displaySize();
			
			g.setColor(new Color(0xbf222222, true));
			g.fill3DRect(d.width / 12, d.height / 12, d.width * 5 / 6, d.height * 5 / 6, true);
			
			g.setColor(new Color(0xcccccc));
			g.setFont(new Font(Font.SERIF, Font.BOLD, d.height / 12));
			String s = "Rows&Cols";
			g.drawString(s, (d.width - (int)g.getFont().getStringBounds(s,
					g.getFontRenderContext()).getWidth()) / 2, d.height / 6);
			g.setFont(new Font(Font.SERIF, Font.BOLD, d.height / 24));
			if (header != null) {
				g.drawString(header,
						(d.width - (int)g.getFont().getStringBounds(header,
						g.getFontRenderContext()).getWidth()) / 2, d.height * 2 / 9);
			}
			
			Rectangle r = new Rectangle(d.width * 7 / 12, d.height * 3 / 12,
					d.width * 3 / 12, d.height * 7 / 12);
			g.draw3DRect(r.x, r.y, r.width, r.height, false);
			s = "Highscores";
			g.drawString(s, r.x + (r.width -
					(int)g.getFont().getStringBounds(s, g.getFontRenderContext()).getWidth()) / 2,
					r.y + d.height / 16);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, d.height / 36));
			int ypos = r.y + d.height / 9;
			int delta = d.height / (2 * highscores.length);
			int sel = 0;
			for (HighscoreEntry e : highscores) {
				if (sel == highlightHighscore) {
					g.setColor(new Color(0x3fcccccc, true));
					g.fill3DRect(r.x + 2, ypos - delta * 2 / 3, r.width - 3, delta, true);
				}
				g.setColor(new Color(0xcccccc));
				s = (e.points * e.nrPlayers) + " (" + e.points + "×" + e.nrPlayers + ")";
				g.drawString(s, r.x + r.width / 2 -
						(int)g.getFont().getStringBounds(s, g.getFontRenderContext()).getWidth(), ypos);
				g.drawString(": " + e.name, r.x + r.width / 2, ypos);
				ypos += delta;
				++sel;
			}
			
			r = new Rectangle(d.width * 2 / 12, d.height * 3 / 12,
					d.width * 3 / 12, d.height * 7 / 12);
			g.draw3DRect(r.x, r.y, r.width, r.height, false);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, d.height / 30));
			ypos = r.y + d.height / 12;
			ArrayList<String> entries = new ArrayList<>();
			entries.add("Load");
			entries.add("Start");
			entries.add(players.size() + " players");
			for (PlayerSetting p : players) {
				entries.add((p.ai ? "⌘ " : "") + p.name);
			}
			entries.add("Quit");
			sel = 0;
			delta = d.height / (2 * entries.size());
			for (String str : entries) {
				if (sel == selectedEntry) {
					if (sel == 2) {
						final int n = players.size();
						if (n > 2) str = "< " + str;
						if (n < 6) str += " >";
					} else if (sel > 2 && sel < 3 + players.size()) {
						str += "_";
					}
					g.setColor(new Color(sel != 0 || canLoad ? 0xcccccccc : 0x44444444, true));
					g.fill3DRect(r.x + 2, ypos - delta * 2 / 3, r.width - 3, delta, true);
					g.setColor(new Color(0x444444));
				} else {
					g.setColor(new Color(sel == 0 && !canLoad ? 0x444444 : 0xcccccc));
				}
				g.drawString(str, r.x + (r.width -
						(int)g.getFont().getStringBounds(str, g.getFontRenderContext()).getWidth()) / 2, ypos);
				ypos += delta;
				++sel;
			}
		}
		private final Game game;
		private final String header;
		private boolean canLoad;
		private final ArrayList<PlayerSetting> players;
		private int selectedEntry;
		private int highlightHighscore;
		public Menu(Game g, String h) {
			game = g;
			header = h;
			highlightHighscore = -1;
			canLoad = kSavegame.isFile();
			selectedEntry = canLoad ? 0 : 1;
			players = new ArrayList<>();
			players.add(new PlayerSetting("Player 1", false));
			players.add(new PlayerSetting("Player 2", true));
			players.add(new PlayerSetting("Player 3", true));
		}
		public void handleKey(KeyEvent k) {
			final int n = players.size();
			switch (k.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					++selectedEntry;
					selectedEntry %= (n + 4);
					if (selectedEntry == 0 && !canLoad) selectedEntry = 1;
					break;
				case KeyEvent.VK_UP:
					selectedEntry += n + 3;
					selectedEntry %= (n + 4);
					if (selectedEntry == 0 && !canLoad) selectedEntry = n + 3;
					break;
				case KeyEvent.VK_RIGHT:
					if (selectedEntry == 2 && n < 6) {
						players.add(new PlayerSetting("Player " + (n + 1), players.get(n - 1).ai));
					}
					break;
				case KeyEvent.VK_LEFT:
					if (selectedEntry == 2 && n > 2) {
						players.remove(n - 1);
					}
					break;
				case KeyEvent.VK_ENTER:
					switch (selectedEntry) {
						case 0:
							if (canLoad) {
								canLoad &= game.newGame(null, null);
							}
							break;
						case 1:
							String[] str = new String[players.size()];
							boolean[] ai = new boolean[str.length];
							for (int i = 0; i < str.length; ++i) {
								PlayerSetting p = players.get(i);
								str[i] = p.name;
								ai[i] = p.ai;
							}
							game.newGame(str, ai);
							break;
						case 2:
							break;
						default:
							if (selectedEntry == n + 3) {
								System.exit(0);
							} else {
								PlayerSetting p = players.get(selectedEntry - 3);
								p.ai = !p.ai;
							}
							break;
					}
					break;
				case KeyEvent.VK_BACK_SPACE:
					if (selectedEntry > 2 && selectedEntry < 3 + n) {
						PlayerSetting p = players.get(selectedEntry - 3);
						if (!p.name.isEmpty()) {
							p.name = p.name.substring(0, p.name.length() - 1);
						}
					}
					break;
				case KeyEvent.VK_ESCAPE:
					System.exit(0);
					break;
				default:
					final char c = k.getKeyChar();
					if (selectedEntry > 2 && selectedEntry < 3 + n &&
							k.getKeyChar() != KeyEvent.CHAR_UNDEFINED &&
							(c == ' ' || Character.isLetterOrDigit(c))) {
						players.get(selectedEntry - 3).name += c;
					}
					break;
			}
		}
		public void handleMousePress(MouseEvent m) {
			
		}
		public void handleMouseMove(MouseEvent m) {
			
		}
	}
	Menu menu;
	
	public Dimension displaySize() {
		return new Dimension(display.getWidth(), display.getHeight());
	}
	
	private static Color[] kBrickColors = new Color[] {
		new Color(0x00007F),
		new Color(0x00BF00),
		new Color(0x7F0000),
		new Color(0xBFBF00),
		new Color(0x007F7F),
		new Color(0xBF00BF),
	};
	
	private static void draw(Graphics2D g, Rectangle r, Brick b) {
		g.setColor(new Color(0x111111));
		g.fill3DRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, true);
		g.setColor(kBrickColors[b.color]);
		switch (b.shape) {
			case 0:
				g.fillRect(r.x + r.width / 4, r.y + r.height / 4, r.width / 2, r.height / 2);
				break;
			case 1:
				g.fillOval(r.x + r.width / 6, r.y + r.height / 6, r.width * 2 / 3, r.height * 2 / 3);
				break;
			case 2:
				g.fillPolygon(
					new int[] {
						r.x + r.width / 2, r.x + r.width * 5 / 6,
						r.x + r.width / 2, r.x + r.width / 6
					}, new int[] {
						r.y + r.height / 6, r.y + r.height / 2,
						r.y + r.height * 5 / 6, r.y + r.height / 2
					}, 4);
				break;
			case 3:
				g.fillPolygon(
					new int[] {
						r.x + r.width / 6,
						r.x + r.width / 2,
						r.x + r.width * 5 / 6,
						r.x + r.width * 3 / 4,
						r.x + r.width * 5 / 6,
						r.x + r.width / 2,
						r.x + r.width / 6,
						r.x + r.width / 4,
					}, new int[] {
						r.y + r.height / 6,
						r.y + r.height / 4,
						r.y + r.height / 6,
						r.y + r.height / 2,
						r.y + r.height * 5 / 6,
						r.y + r.height * 3 / 4,
						r.y + r.height * 5 / 6,
						r.y + r.height / 2,
					}, 8);
				break;
			case 4:
				for (int i = 1; i < 6; ++i) {
					g.drawLine(r.x + r.width * i / 6, r.y + r.height / 6,
							r.x + r.width * (6 - i) / 6, r.y + r.height * 5 / 6);
					g.drawLine(r.x + r.width / 6, r.y + r.height * i / 6,
							r.x + r.width * 5 / 6, r.y + r.height * (6 - i) / 6);
				}
				break;
			case 5:
				g.fillOval(r.x + r.width / 6, r.y + r.height / 3, r.width / 3, r.height / 3);
				g.fillOval(r.x + r.width / 2, r.y + r.height / 3, r.width / 3, r.height / 3);
				g.fillOval(r.x + r.width / 3, r.y + r.height / 6, r.width / 3, r.height / 3);
				g.fillOval(r.x + r.width / 3, r.y + r.height / 2, r.width / 3, r.height / 3);
				break;
			default: throw new RuntimeException("Invalid brick shape: " + b.shape);
		}
	}
	
	private Point coords(Point mouse) {
		final Metrics dim = boardDimension();
		return new Point(dim.minX + mouse.x / dim.brickSize, dim.minY + mouse.y / dim.brickSize);
	}
	
	private static class Metrics {
		final int minX, minY, maxX, maxY, brickSize;
		public Metrics(int i1, int i2, int i3, int i4, int s) {
			minX = i1;
			minY = i2;
			maxX = i3;
			maxY = i4;
			brickSize = s;
		}
	}
	private Metrics boardDimension() {
		int minX = -3;
		int minY = -2;
		int maxX = 4;
		int maxY = 3;
		for (Point p : board.keySet()) {
			minX = Math.min(minX, p.x);
			maxX = Math.max(maxX, p.x);
			minY = Math.min(minY, p.y);
			maxY = Math.max(maxY, p.y);
		}
		--minX;
		maxX += 2;
		minY -= 2;
		++maxY;
		return new Metrics(minX, minY, maxX, maxY,
				Math.min(display.getWidth() / (1 + maxX - minX), display.getHeight() / (1 + maxY - minY)));
	}
	
	private static final Color kColorOk = new Color(0x7f3fff3f, true);
	private static final Color kColorCancel = new Color(0x7f000000, true);
	public synchronized void draw() {
		final int w = display.getWidth();
		final int h = display.getHeight();
		final int whm = Math.max(w, h);
		
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		
		for (int i = 0; i < whm * 2; i++) {
			int c = 255 * i / (whm * 2);
			g.setColor(new Color(c, c, c));
			if (w > h)
				g.drawLine(i, 0, 0, i * h / w);
			else
				g.drawLine(i * w / h, 0, 0, i);
		}
		
		final Metrics dim = boardDimension();
		for (Point p : board.keySet()) {
			Rectangle r = new Rectangle(dim.brickSize * (p.x - dim.minX), dim.brickSize * (p.y - dim.minY),
					dim.brickSize, dim.brickSize);
			draw(g, r, board.get(p));
			if (bricksPlaced.contains(p)) {
				g.setColor(new Color(0x10bfbfbf, true));
				g.fill(r);
			}
		}
		int playerToShowBricksFor;
		if (!players[currentPlayer].ai) {
			playerToShowBricksFor = currentPlayer;
		} else {
			int nrAIs = 0; for (Player p : players) { if (p.ai) { ++nrAIs; }}
			if (nrAIs == players.length - 1) {
				for (int i = 0;; ++i) {
					if (!players[i].ai) {
						playerToShowBricksFor = i;
						break;
					}
				}
			} else if (nrAIs == players.length) {
				playerToShowBricksFor = currentPlayer;
			} else {
				playerToShowBricksFor = -1;
			}
		}
		if (playerToShowBricksFor >= 0) {
			g.setColor(new Color(0x7f222222, true));
			g.fill3DRect(w - dim.brickSize * 9 / 8, dim.brickSize * 7 / 8,
					dim.brickSize * 9 / 8, dim.brickSize * 50 / 8, false);
			int bi = 0;
			for (Brick b : players[playerToShowBricksFor].getBricks()) {
				brickPositions[bi] = new Rectangle(w - dim.brickSize * 17 / 16, dim.brickSize * (bi + 1),
						dim.brickSize, dim.brickSize);
				if (b != null) {
					draw(g, brickPositions[bi], b);
				}
				++bi;
			}
		}
		
		final int fontSize = dim.brickSize / (players.length + 1);
		g.setColor(new Color(0xcccccc));
		for (int i = 0; i < players.length; ++i) {
			g.setFont(new Font(Font.SANS_SERIF, i == currentPlayer ? Font.BOLD : Font.PLAIN, fontSize));
			final String s = players[i].name + " (" + players[i].getPoints() + ") ";
			g.drawString(s, w - (int)g.getFont().getStringBounds(s, g.getFontRenderContext()).getWidth(),
					(i + 1) * fontSize);
		}
		
		final Font kFontSignum = new Font(Font.SANS_SERIF, Font.BOLD, dim.brickSize / 2);
		final Font kFontText = new Font(Font.SANS_SERIF, Font.ITALIC, dim.brickSize / 7);
		final Color kTextEnabled = new Color(0xdddddd);
		final Color kTextDisabled = new Color(0xa0444444, true);
		final boolean enableButtonNext = !bricksPlaced.isEmpty() ||
				(swappingBricks != null && !swappingBricks.isEmpty());
		final boolean enableButtonUndo = !players[currentPlayer].ai && enableButtonNext;
		final boolean enableButtonSwap = !players[currentPlayer].ai && bricksPlaced.isEmpty() &&
				((swappingBricks == null && !remainingBricks.isEmpty()) ||
				(swappingBricks != null && swappingBricks.isEmpty()));
		final boolean enableButtonAI = players[currentPlayer].ai && !enableButtonNext;
		for (int i = 0; i < 4; ++i) {
			g.draw3DRect((2 * i + 1) * dim.brickSize + 1, 1, 2 * dim.brickSize - 2, dim.brickSize - 2, true);
		}
		
		g.setColor(enableButtonNext ? kTextEnabled : kTextDisabled);
		buttonNext = new Rectangle(dim.brickSize, 0, 2 * dim.brickSize, dim.brickSize);
		String str = "↪";
		Rectangle2D r = kFontSignum.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontSignum);
		g.drawString(str, buttonNext.x + (buttonNext.width - (int)r.getWidth()) / 2,
				(buttonNext.height + (int)r.getHeight()) / 2);
		str = "Next player";
		r = kFontText.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontText);
		g.drawString(str, buttonNext.x + (buttonNext.width - (int)r.getWidth()) / 2, (int)r.getHeight() + 1);
		
		g.setColor(enableButtonUndo ? kTextEnabled : kTextDisabled);
		buttonUndo = new Rectangle(3 * dim.brickSize, 0, 2 * dim.brickSize, dim.brickSize);
		str = "↶";
		r = kFontSignum.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontSignum);
		g.drawString(str, buttonUndo.x + (buttonUndo.width - (int)r.getWidth()) / 2,
				(buttonUndo.height + (int)r.getHeight()) / 2);
		str = "Undo moves";
		r = kFontText.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontText);
		g.drawString(str, buttonUndo.x + (buttonUndo.width - (int)r.getWidth()) / 2, (int)r.getHeight() + 1);
		
		g.setColor(enableButtonSwap ? kTextEnabled : kTextDisabled);
		buttonSwap = new Rectangle(5 * dim.brickSize, 0, 2 * dim.brickSize, dim.brickSize);
		str = !enableButtonSwap || swappingBricks == null ? "⇵" : "⌧";
		r = kFontSignum.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontSignum);
		g.drawString(str, buttonSwap.x + (buttonSwap.width - (int)r.getWidth()) / 2,
				(buttonSwap.height + (int)r.getHeight()) / 2);
		str = !enableButtonSwap || swappingBricks == null ? "Swap bricks" : "Cancel swapping";
		r = kFontText.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontText);
		g.drawString(str, buttonSwap.x + (buttonSwap.width - (int)r.getWidth()) / 2, (int)r.getHeight() + 1);
		
		g.setColor(enableButtonAI ? kTextEnabled : kTextDisabled);
		buttonAI = new Rectangle(7 * dim.brickSize, 0, 2 * dim.brickSize, dim.brickSize);
		str = "⌘";
		r = kFontSignum.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontSignum);
		g.drawString(str, buttonAI.x + (buttonAI.width - (int)r.getWidth()) / 2,
				(buttonAI.height + (int)r.getHeight()) / 2);
		str = "AI step";
		r = kFontText.getStringBounds(str, g.getFontRenderContext());
		g.setFont(kFontText);
		g.drawString(str, buttonAI.x + (buttonAI.width - (int)r.getWidth()) / 2, (int)r.getHeight() + 1);
		
		if (!enableButtonSwap || menu != null) buttonSwap = null;
		if (!enableButtonAI || menu != null) buttonAI = null;
		if (!enableButtonNext || menu != null) buttonNext = null;
		if (!enableButtonUndo || menu != null) buttonUndo = null;
		
		if (!gameOver) {
			g.setColor(kTextEnabled);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, dim.brickSize / 4));
			str = "+" + remainingBricks.size();
			g.drawString(str, (dim.brickSize - (int)g.getFont().getStringBounds(str,
					g.getFontRenderContext()).getWidth()) / 2, dim.brickSize * 2 / 3);
		}
		
		if (currentBrick != null) {
			boolean onBrickLoc = false;
			for (int i = 0; i < brickPositions.length; ++i) {
				if (players[currentPlayer].getBricks()[i] == null &&
						brickPositions[i].contains(currentBrickPosition)) {
					g.setColor(kColorOk);
					g.fill(brickPositions[i]);
					onBrickLoc = true;
					break;
				}
			}
			if (!onBrickLoc) {
				final Point c = coords(currentBrickPosition);
				g.setColor(mayPlaceAt(c) ? kColorOk : kColorCancel);
				g.fillRect(dim.brickSize * (c.x - dim.minX), dim.brickSize * (c.y - dim.minY),
						dim.brickSize, dim.brickSize);
			}
			
			draw(g, new Rectangle(currentBrickPosition.x - dim.brickSize / 2,
					currentBrickPosition.y - dim.brickSize / 2, dim.brickSize, dim.brickSize), currentBrick);
		}
		
		if (menu != null) {
			menu.draw(g);
		}
		
		display.setIcon(new ImageIcon(img));
	}
	
	public Map<Point, Brick> getBoard() {
		return new HashMap<>(board);
	}
	
	public boolean mayPlaceAt(Point p) {
		return mayPlaceAt(currentBrick, board, bricksPlaced, p);
	}
	
	public static boolean mayPlaceAt(Brick currentBrick, Map<Point, Brick> board,
			ArrayList<Point> bricksPlaced, Point p) {
		if (board.get(p) != null) {
			return false;
		}
		for (Point point : bricksPlaced) {
			if (point.x == p.x) {
				if (board.get(new Point(point.x, point.y + 1)) == null &&
						board.get(new Point(point.x, point.y - 1)) == null) {
					return false;
				}
				for (int y = Math.min(p.y, point.y); y < p.y || y < point.y; ++y) {
					if (y != p.y && board.get(new Point(p.x, y)) == null) {
						return false;
					}
				}
			} else if (point.y == p.y) {
				if (board.get(new Point(point.x + 1, point.y)) == null &&
						board.get(new Point(point.x - 1, point.y)) == null) {
					return false;
				}
				for (int x = Math.min(p.x, point.x); x < p.x || x < point.x; ++x) {
					if (x != p.x && board.get(new Point(x, p.y)) == null) {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		final Brick bn = board.get(new Point(p.x, p.y - 1));
		final Brick bs = board.get(new Point(p.x, p.y + 1));
		final Brick bw = board.get(new Point(p.x - 1, p.y));
		final Brick be = board.get(new Point(p.x + 1, p.y));
		if (bn == null && bw == null && be == null && bs == null) {
			return false;
		}
		if (bw != null) {
			final boolean sameColor = currentBrick.color == bw.color;
			final boolean sameShape = currentBrick.shape == bw.shape;
			if (sameColor == sameShape) {
				return false;
			}
			Point point = new Point(p.x - 1, p.y);
			for (Brick brick = board.get(point); brick != null; --point.x, brick = board.get(point)) {
				if ((sameColor && (currentBrick.color != brick.color || currentBrick.shape == brick.shape)) ||
						(sameShape && (currentBrick.color == brick.color || currentBrick.shape != brick.shape))) {
					return false;
				}
			}
		}
		if (be != null) {
			final boolean sameColor = currentBrick.color == be.color;
			final boolean sameShape = currentBrick.shape == be.shape;
			if (sameColor == sameShape) {
				return false;
			}
			Point point = new Point(p.x + 1, p.y);
			for (Brick brick = board.get(point); brick != null; ++point.x, brick = board.get(point)) {
				if ((sameColor && (currentBrick.color != brick.color || currentBrick.shape == brick.shape)) ||
						(sameShape && (currentBrick.color == brick.color || currentBrick.shape != brick.shape))) {
					return false;
				}
			}
		}
		if (bn != null) {
			final boolean sameColor = currentBrick.color == bn.color;
			final boolean sameShape = currentBrick.shape == bn.shape;
			if (sameColor == sameShape) {
				return false;
			}
			Point point = new Point(p.x, p.y - 1);
			for (Brick brick = board.get(point); brick != null; --point.y, brick = board.get(point)) {
				if ((sameColor && (currentBrick.color != brick.color || currentBrick.shape == brick.shape)) ||
						(sameShape && (currentBrick.color == brick.color || currentBrick.shape != brick.shape))) {
					return false;
				}
			}
		}
		if (bs != null) {
			final boolean sameColor = currentBrick.color == bs.color;
			final boolean sameShape = currentBrick.shape == bs.shape;
			if (sameColor == sameShape) {
				return false;
			}
			Point point = new Point(p.x, p.y + 1);
			for (Brick brick = board.get(point); brick != null; ++point.y, brick = board.get(point)) {
				if ((sameColor && (currentBrick.color != brick.color || currentBrick.shape == brick.shape)) ||
						(sameShape && (currentBrick.color == brick.color || currentBrick.shape != brick.shape))) {
					return false;
				}
			}
		}
		if (bw != null && be != null) {
			final boolean sameColorW = currentBrick.color == bw.color;
			final boolean sameShapeW = currentBrick.shape == bw.shape;
			final boolean sameColorE = currentBrick.color == be.color;
			final boolean sameShapeE = currentBrick.shape == be.shape;
			if (sameColorW == sameShapeW || sameColorE == sameShapeE ||
					sameColorW != sameColorE || sameShapeW != sameShapeE) {
				return false;
			}
			Point p1 = new Point(p.x - 1, p.y);
			for (Brick b1 = board.get(p1); b1 != null; --p1.x, b1 = board.get(p1)) {
				Point p2 = new Point(p.x + 1, p.y);
				for (Brick b2 = board.get(p2); b2 != null; ++p2.x, b2 = board.get(p2)) {
					if ((sameColorW && (b1.color != b2.color || b1.shape == b2.shape)) ||
							(sameShapeW && (b1.color == b2.color || b1.shape != b2.shape))) {
						return false;
					}
				}
			}
		}
		if (bn != null && bs != null) {
			final boolean sameColorN = currentBrick.color == bn.color;
			final boolean sameShapeN = currentBrick.shape == bn.shape;
			final boolean sameColorS = currentBrick.color == bs.color;
			final boolean sameShapeS = currentBrick.shape == bs.shape;
			if (sameColorN == sameShapeN || sameColorS == sameShapeS ||
					sameColorN != sameColorS || sameShapeN != sameShapeS) {
				return false;
			}
			Point p1 = new Point(p.x, p.y - 1);
			for (Brick b1 = board.get(p1); b1 != null; --p1.y, b1 = board.get(p1)) {
				Point p2 = new Point(p.x, p.y + 1);
				for (Brick b2 = board.get(p2); b2 != null; ++p2.y, b2 = board.get(p2)) {
					if ((sameColorN && (b1.color != b2.color || b1.shape == b2.shape)) ||
							(sameShapeN && (b1.color == b2.color || b1.shape != b2.shape))) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void nextPlayer() {
		if (!bricksPlaced.isEmpty()) {
			ArrayList<Point[]> rowsToCount = new ArrayList<>();
			for (Point p : bricksPlaced) {
				int xMin = p.x;
				int xMax = p.x;
				int yMin = p.y;
				int yMax = p.y;
				for (; board.get(new Point(xMin, p.y)) != null; --xMin);
				for (; board.get(new Point(p.x, yMin)) != null; --yMin);
				for (; board.get(new Point(xMax, p.y)) != null; ++xMax);
				for (; board.get(new Point(p.x, yMax)) != null; ++yMax);
				assert(xMax > xMin);
				assert(yMax > yMin);
				++xMin;
				++yMin;
				if (xMax - xMin > 1) {
					Point[] rx = new Point[xMax - xMin];
					for (int x = xMin; x < xMax; ++x) rx[x - xMin] = new Point(x, p.y);
					boolean cont = false;
					for (Point[] r : rowsToCount) {
						if (r.length != rx.length) continue;
						cont = true;
						for (int i = 0; cont && i < r.length; ++i) {
							if (r[i].x != rx[i].x || r[i].y != rx[i].y) {
								cont = false;
								break;
							}
						}
						if (cont) break;
					}
					if (!cont) rowsToCount.add(rx);
				}
				if (yMax - yMin > 1) {
					Point[] ry = new Point[yMax - yMin];
					for (int y = yMin; y < yMax; ++y) ry[y - yMin] = new Point(p.x, y);
					boolean cont = false;
					for (Point[] r : rowsToCount) {
						if (r.length != ry.length) continue;
						cont = true;
						for (int i = 0; cont && i < r.length; ++i) {
							if (r[i].x != ry[i].x || r[i].y != ry[i].y) {
								cont = false;
								break;
							}
						}
						if (cont) break;
					}
					if (!cont) rowsToCount.add(ry);
				}
			}
			for (Point[] p : rowsToCount) {
				assert(p.length > 1);
				assert(p.length <= 6);
				players[currentPlayer].addPoints(p.length * (p.length < 6 ? 1 : 2));
			}
		}
		boolean allEmpty = true;
		for (int i = 0; i < Player.kBricksPerPlayer; ++i) {
			if (players[currentPlayer].getBricks()[i] == null) {
				Brick b = null;
				if (currentBrick != null) {
					b = currentBrick;
					currentBrick = null;
				} else if (!remainingBricks.isEmpty()) {
					b = remainingBricks.remove(0);
				}
				if (b != null) {
					allEmpty = false;
					players[currentPlayer].getBricks()[i] = b;
				}
			} else {
				allEmpty = false;
			}
		}
		if (allEmpty) {
			gameOver = true;
			players[currentPlayer].addPoints(6);
			long winner = 0;
			for (Player p : players) winner = Math.max(winner, p.getPoints());
			ArrayList<String> winnerNames = new ArrayList<>();
			for (Player p : players) {
				if (p.getPoints() >= winner) {
					winnerNames.add(p.name);
				}
			}
			String str;
			final int nrWinners = winnerNames.size();
			if (nrWinners == 1) {
				str = winnerNames.get(0) + " has won!";
			} else {
				str = "";
				for (int i = 0; i < nrWinners - 1; ++i) str += ", " + winnerNames.get(i);
				str += " and " + winnerNames.get(nrWinners - 1) + " are joint winners!";
			}
			menu = new Menu(this, str);
			if (nrWinners == 1) {
				menu.registerHighscore(new Menu.HighscoreEntry(winnerNames.get(0), winner, players.length));
			}
			draw();
			return;
		}
		if (swappingBricks != null) {
			int s = remainingBricks.size();
			for (Brick b : swappingBricks) {
				remainingBricks.add((int)(Math.random() * ++s), b);
			}
		}
		++currentPlayer;
		currentPlayer %= players.length;
		bricksPlaced.clear();
		swappingBricks = null;
		currentBrickPosition = new Point(-1, -1);
	}
	
	private static final File kSavegame = new File(".save");
	private static final File kHighscores = new File(".scores");
	
	private boolean newGame(String[] plNames, boolean[] ai) {
		gameNewlyStarted = true;
		gameOver = false;
		currentPlayer = 0;
		board.clear();
		remainingBricks.clear();
		bricksPlaced.clear();
		swappingBricks = null;
		currentBrick = null;
		currentBrickPosition = new Point(-1, -1);
		buttonSwap = null;
		buttonNext = null;
		buttonAI = null;
		buttonUndo = null;
		if (plNames == null || ai == null) {
			try {
				java.util.List<String> save = Files.readAllLines(kSavegame.toPath());
				int i = 0;
				String[] data = save.get(i++).split(" ");
				players = new Player[Integer.valueOf(data[0])];
				currentPlayer = Integer.valueOf(data[1]);
				for (int p = 0; p < players.length; ++p) {
					final String n = save.get(i++);
					data = save.get(i++).split(" ");
					int j = 0;
					final boolean a = data[j++].equals("1");
					players[p] = new Player(n, a);
					players[p].addPoints(Long.valueOf(data[j++]));
					for (int b = 0; b < Player.kBricksPerPlayer; ++b) {
						if (data[j++].equals("1")) {
							final int s = Integer.valueOf(data[j++]);
							final int c = Integer.valueOf(data[j++]);
							players[p].getBricks()[b] = new Brick(c, s);
						}
					}
				}
				data = save.get(i++).split(" ");
				int j = 0;
				final int boardS = Integer.valueOf(data[j++]);
				final int remBrS = Integer.valueOf(data[j++]);
				final int brPlacedS = Integer.valueOf(data[j++]);
				final int swapS = Integer.valueOf(data[j++]);
				if (data[j++].equals("0")) {
					currentBrick = null;
				} else {
					final int s = Integer.valueOf(data[j++]);
					final int c = Integer.valueOf(data[j++]);
					currentBrick = new Brick(c, s);
				}
				for (int b = 0; b < boardS; ++b) {
					final int x = Integer.valueOf(data[j++]);
					final int y = Integer.valueOf(data[j++]);
					final int s = Integer.valueOf(data[j++]);
					final int c = Integer.valueOf(data[j++]);
					board.put(new Point(x, y), new Brick(c, s));
				}
				for (int b = 0; b < remBrS; ++b) {
					final int s = Integer.valueOf(data[j++]);
					final int c = Integer.valueOf(data[j++]);
					remainingBricks.add(new Brick(c, s));
				}
				for (int b = 0; b < brPlacedS; ++b) {
					final int x = Integer.valueOf(data[j++]);
					final int y = Integer.valueOf(data[j++]);
					bricksPlaced.add(new Point(x, y));
				}
				if (swapS < 0) {
					swappingBricks = null;
				} else {
					swappingBricks = new ArrayList<>();
					for (int b = 0; b < swapS; ++b) {
						final int s = Integer.valueOf(data[j++]);
						final int c = Integer.valueOf(data[j++]);
						swappingBricks.add(new Brick(c, s));
					}
				}
				gameNewlyStarted = false;
				kSavegame.delete();
				menu = null;
				return true;
			} catch (Exception x) {
				return false;
			}
		}
		int n = 0;
		for (int i = 0; i < 6; ++i) {
			for (int j = 0; j < 6; ++j) {
				for (int k = 0; k < 3; ++k) {
					remainingBricks.add((int)(Math.random() * ++n), new Brick(i, j));
				}
			}
		}
		board.put(new Point(), remainingBricks.remove(0));
		players = new Player[plNames.length];
		for (int i = 0; i < players.length; ++i) {
			players[i] = new Player(plNames[i], ai[i]);
			for (int j = 0; j < Player.kBricksPerPlayer; ++j) {
				players[i].getBricks()[j] = remainingBricks.remove(0);
			}
		}
		menu = null;
		return true;
	}
	
	private void save() {
		if (gameOver || gameNewlyStarted) {
			return;
		}
		try {
			PrintWriter w = new PrintWriter(kSavegame);
			w.println(players.length + " " + currentPlayer);
			for (Player p : players) {
				w.println(p.name);
				w.print((p.ai ? 1 : 0) + " " + p.getPoints());
				for (Brick b : p.getBricks()) {
					w.print(" " + (b == null ? 0 : (1 + " " + b.shape + " " + b.color)));
				}
				w.println();
			}
			w.print(board.size() + " " + remainingBricks.size() + " " +
					bricksPlaced.size() + " " + (swappingBricks == null ? -1 : swappingBricks.size()) + " " +
					(currentBrick == null ? 0 : "1 " + currentBrick.shape + " " + currentBrick.color));
			for (Point p : board.keySet()) {
				Brick b = board.get(p);
				w.print(" " + p.x + " " + p.y + " " + b.shape + " " + b.color);
			}
			for (Brick b : remainingBricks) {
				w.print(" " + b.shape + " " + b.color);
			}
			for (Point p : bricksPlaced) {
				w.print(" " + p.x + " " + p.y);
			}
			if (swappingBricks != null) {
				for (Brick b : swappingBricks) {
					w.print(" " + b.shape + " " + b.color);
				}
			}
			w.close();
		} catch (Exception x) {
			System.out.println("ERROR: Could not save game: " + x);
		}
	}
	
	public Game() {
		frame = new JFrame("Rows&Cols");
		display = new JLabel();
		
		board = new HashMap<>();
		remainingBricks = new ArrayList<>();
		bricksPlaced = new ArrayList<>();
		brickPositions = new Rectangle[Player.kBricksPerPlayer];
		
		newGame(new String[]{""}, new boolean[]{true});
		menu = new Menu(this, null);
		
		display.setPreferredSize(new Dimension(800, 600));
		
		frame.add(display);
		MouseAdapter mouseAdapter = new MouseAdapter() {
			public void mousePressed(MouseEvent m) {
				if (menu != null) {
					menu.handleMousePress(m);
				} else if (buttonNext != null && buttonNext.contains(m.getPoint())) {
					nextPlayer();
				} else if (buttonAI != null && buttonAI.contains(m.getPoint())) {
					gameNewlyStarted = false;
					Player.Move move = players[currentPlayer].aiStep(Game.this);
					if (move instanceof Player.SwapMove) {
						Player.SwapMove sm = (Player.SwapMove)move;
						int n = sm.bricks.length;
						JOptionPane.showMessageDialog(frame,
								players[currentPlayer].name + " swaps " + n + (n == 1 ? " brick." : " bricks."),
								players[currentPlayer].name, JOptionPane.INFORMATION_MESSAGE);
						swappingBricks = new ArrayList<>();
						for (int i : sm.bricks) {
							swappingBricks.add(players[currentPlayer].getBricks()[i]);
							players[currentPlayer].getBricks()[i] = null;
						}
					} else {
						Player.DefaultMove dm = (Player.DefaultMove)move;
						for (Point p : dm.bricks.keySet()) {
							bricksPlaced.add(p);
							board.put(p, players[currentPlayer].getBricks()[dm.bricks.get(p)]);
							players[currentPlayer].getBricks()[dm.bricks.get(p)] = null;
						}
					}
				} else if (buttonSwap != null && buttonSwap.contains(m.getPoint())) {
					gameNewlyStarted = false;
					if (swappingBricks == null) {
						swappingBricks = new ArrayList<>();
						if (currentBrick != null) {
							swappingBricks.add(currentBrick);
							currentBrick = null;
						}
					} else {
						swappingBricks = null;
					}
				} else if (buttonUndo != null && buttonUndo.contains(m.getPoint())) {
					gameNewlyStarted = false;
					if (swappingBricks != null) {
						for (Brick b : swappingBricks) {
							for (int i = 0;; ++i) {
								if (players[currentPlayer].getBricks()[i] == null) {
									players[currentPlayer].getBricks()[i] = b;
									break;
								}
							}
						}
						swappingBricks = null;
					}
					for (Point p : bricksPlaced) {
						Brick b = board.remove(p);
						for (int i = 0;; ++i) {
							if (players[currentPlayer].getBricks()[i] == null) {
								players[currentPlayer].getBricks()[i] = b;
								break;
							}
						}
					}
					bricksPlaced.clear();
				} else {
					boolean picked = false;
					for (int i = 0; i < brickPositions.length; ++i) {
						if (brickPositions[i] != null && brickPositions[i].contains(m.getPoint())) {
							Brick b = players[currentPlayer].getBricks()[i];
							if (swappingBricks == null) {
								players[currentPlayer].getBricks()[i] = currentBrick;
								currentBrick = b;
							} else if (b != null && swappingBricks.size() < remainingBricks.size()) {
								swappingBricks.add(b);
								players[currentPlayer].getBricks()[i] = null;
							}
							picked = true;
							gameNewlyStarted = false;
							break;
						}
					}
					if (!picked && currentBrick != null) {
						final Point c = coords(m.getPoint());
						if (mayPlaceAt(c)) {
							board.put(c, currentBrick);
							bricksPlaced.add(c);
							currentBrick = null;
							gameNewlyStarted = false;
						}
					}
				}
				draw();
			}
			public void mouseMoved(MouseEvent m) {
				if (menu != null) {
					menu.handleMouseMove(m);
					draw();
					return;
				}
				if (players[currentPlayer].ai) {
					return;
				}
				currentBrickPosition = m.getPoint();
				if (currentBrick != null) {
					draw();
				}
			}
		};
		display.addMouseListener(mouseAdapter);
		display.addMouseMotionListener(mouseAdapter);
		display.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (menu != null) {
					menu.handleKey(e);
				} else {
					switch (e.getKeyCode()) {
						case KeyEvent.VK_ENTER:
						case KeyEvent.VK_SPACE:
							for (Rectangle ab : new Rectangle[] { buttonAI, buttonNext, buttonSwap, buttonUndo }) {
								if (ab != null) {
									mouseAdapter.mousePressed(new MouseEvent(display, MouseEvent.MOUSE_PRESSED, 0, 0,
											ab.x + ab.width / 2, ab.y + ab.height / 2, 1, false));
								}
							}
							break;
						case KeyEvent.VK_ESCAPE:
							gameNewlyStarted = false;
							save();
							menu = new Menu(Game.this, "Paused");
							break;
						default:
							break;
					}
				}
				draw();
			}
		});
		display.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				draw();
			}
		});
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				save();
				System.exit(0);
			}
		});
		display.setFocusable(true);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
	}
	
	public static void main(String[] args) {
		new Game();
	}
	
}
