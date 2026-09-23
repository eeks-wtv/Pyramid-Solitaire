import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * The PyramidSolitaireGUI class provides a Swing-based graphical interface
 * for playing Pyramid Solitaire.
 *
 * Layout:
 *   - A header bar with the title, a short rules hint, card counts and a New Game button
 *   - The pyramid in the middle, with rows overlapping like a real table layout
 *   - The stock and waste piles centred underneath
 *
 * Mouse clicks allow the user to:
 *   - Draw from stock -> waste
 *   - Select/deselect the waste
 *   - Select two free pyramid cards to remove (sum = 13)
 *   - Click a free pyramid King to remove it alone
 *   - Pair a free pyramid card with the waste to remove (sum = 13)
 *
 * Free cards light up when hovered, covered cards are slightly dimmed, and
 * the win/lose message appears as an overlay inside the window.
 */
public class PyramidSolitaireGUI extends JFrame {
    // ---- Window & layout ----
    private static final int WINDOW_WIDTH = 860;
    private static final int WINDOW_HEIGHT = 820;
    private static final int HEADER_HEIGHT = 64;

    private static final int CW = card.CARD_WIDTH;
    private static final int CH = card.CARD_HEIGHT;
    private static final int ARC = card.CORNER_ARC;

    private static final int X_GAP = CW + 12;   // horizontal distance between cards in a row
    private static final int Y_GAP = 62;        // vertical distance between rows (rows overlap)
    private static final int PYRAMID_TOP = HEADER_HEIGHT + 28;
    private static final int PILES_Y = PYRAMID_TOP + (PyramidSolitaireGame.ROWS - 1) * Y_GAP + CH + 36;
    private static final int PILE_SPACING = 50; // gap between stock and waste

    private static final Rectangle NEW_GAME_BUTTON =
            new Rectangle(WINDOW_WIDTH - 24 - 130, (HEADER_HEIGHT - 36) / 2, 130, 36);

    // ---- Colours ----
    private static final Color FELT_CENTER   = new Color(30, 120, 70);
    private static final Color FELT_EDGE     = new Color(8, 60, 34);
    private static final Color HEADER_TOP    = new Color(12, 45, 28);
    private static final Color HEADER_BOTTOM = new Color(7, 30, 18);
    private static final Color GOLD          = new Color(212, 170, 60);
    private static final Color GOLD_HOVER    = new Color(235, 195, 85);
    private static final Color TEXT_LIGHT    = new Color(240, 240, 230);
    private static final Color TEXT_MUTED    = new Color(185, 212, 192);

    private final PyramidSolitaireGame game; // the game logic/state
    private final GamePanel panel;           // custom JPanel for drawing

    // Selection tracking
    private int selRow = -1, selCol = -1;
    private boolean wasteSelected = false;

    /** Constructor: set up frame properties, instantiate game, and show GUI. */
    public PyramidSolitaireGUI() {
        super("Pyramid Solitaire");

        game = new PyramidSolitaireGame();
        panel = new GamePanel();
        setContentPane(panel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---- Layout helpers ----

    /** @return the x-coordinate of the card at row r, column c (rows are centred). */
    private static int cardX(int r, int c) {
        int rowWidth = r * X_GAP + CW;
        return WINDOW_WIDTH / 2 - rowWidth / 2 + c * X_GAP;
    }

    /** @return the y-coordinate of row r. */
    private static int cardY(int r) {
        return PYRAMID_TOP + r * Y_GAP;
    }

    private static Rectangle stockRect() {
        return new Rectangle(WINDOW_WIDTH / 2 - PILE_SPACING / 2 - CW, PILES_Y, CW, CH);
    }

    private static Rectangle wasteRect() {
        return new Rectangle(WINDOW_WIDTH / 2 + PILE_SPACING / 2, PILES_Y, CW, CH);
    }

    /**
     * Inner class GamePanel does all custom drawing and mouse handling.
     */
    private class GamePanel extends JPanel {
        // Hover tracking (for highlights and the hand cursor)
        private int hoverRow = -1, hoverCol = -1;
        private boolean hoverStock = false, hoverWaste = false, hoverButton = false;

        // When the game ends, these hold the overlay text (null while playing)
        private String gameOverTitle = null;
        private String gameOverSubtitle = null;

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    handleClick(e.getX(), e.getY());
                    updateHover(e.getX(), e.getY());
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (updateHover(e.getX(), e.getY())) {
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverRow = hoverCol = -1;
                    hoverStock = hoverWaste = hoverButton = false;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        // =====================================================================
        // Drawing
        // =====================================================================

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            drawFelt(g2);
            drawHeader(g2);
            drawPyramid(g2);
            drawPiles(g2);
            if (gameOverTitle != null) {
                drawGameOverOverlay(g2);
            }
            g2.dispose();
        }

        /** Green felt table with a soft spotlight in the middle. */
        private void drawFelt(Graphics2D g2) {
            float radius = Math.max(WINDOW_WIDTH, WINDOW_HEIGHT) * 0.75f;
            RadialGradientPaint felt = new RadialGradientPaint(
                    new Point2D.Float(WINDOW_WIDTH / 2f, WINDOW_HEIGHT * 0.45f),
                    radius,
                    new float[] {0f, 1f},
                    new Color[] {FELT_CENTER, FELT_EDGE});
            g2.setPaint(felt);
            g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        }

        /** Header bar: title, rules hint, card counts, New Game button. */
        private void drawHeader(Graphics2D g2) {
            g2.setPaint(new GradientPaint(0, 0, HEADER_TOP, 0, HEADER_HEIGHT, HEADER_BOTTOM));
            g2.fillRect(0, 0, WINDOW_WIDTH, HEADER_HEIGHT);
            g2.setColor(new Color(212, 170, 60, 120));
            g2.fillRect(0, HEADER_HEIGHT - 2, WINDOW_WIDTH, 2);

            // Title and hint
            g2.setFont(new Font("Serif", Font.BOLD, 24));
            g2.setColor(TEXT_LIGHT);
            g2.drawString("Pyramid Solitaire", 24, 32);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(TEXT_MUTED);
            g2.drawString("Pair free cards that add up to 13 \u00B7 Kings clear on their own", 24, 50);

            // Card counts, right-aligned next to the button
            String stats = "Pyramid " + countPyramidLeft() + "   \u00B7   Stock " + game.getStockSize();
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int statsX = NEW_GAME_BUTTON.x - 24 - fm.stringWidth(stats);
            int statsY = HEADER_HEIGHT / 2 + fm.getAscent() / 2 - 2;
            g2.setColor(TEXT_LIGHT);
            g2.drawString(stats, statsX, statsY);

            // New Game button
            Rectangle b = NEW_GAME_BUTTON;
            g2.setColor(hoverButton ? GOLD_HOVER : GOLD);
            g2.fillRoundRect(b.x, b.y, b.width, b.height, 18, 18);
            g2.setColor(new Color(40, 30, 5));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            drawCentered(g2, "New Game", b);
        }

        /**
         * Draws the pyramid top row first, so each lower row overlaps the
         * bottom of the row above it.  Covered cards are dimmed slightly;
         * a hovered free card gets a white glow.
         */
        private void drawPyramid(Graphics2D g2) {
            for (int r = 0; r < PyramidSolitaireGame.ROWS; r++) {
                for (int c = 0; c <= r; c++) {
                    card cd = game.getPyramidCard(r, c);
                    if (cd == null || cd.isRemoved()) {
                        continue;
                    }
                    int x = cardX(r, c);
                    int y = cardY(r);

                    drawShadow(g2, x, y);
                    cd.setSelected(selRow == r && selCol == c && !wasteSelected);
                    cd.draw(g2, x, y);

                    if (!game.isFree(r, c)) {
                        g2.setColor(new Color(0, 0, 0, 55));
                        g2.fillRoundRect(x, y, CW, CH, ARC, ARC);
                    } else if (r == hoverRow && c == hoverCol && !cd.isSelected()) {
                        drawGlow(g2, x, y);
                    }
                }
            }
        }

        /** Stock (as a small stacked pile with a count badge) and waste, with labels. */
        private void drawPiles(Graphics2D g2) {
            Rectangle s = stockRect();
            Rectangle w = wasteRect();
            int stockSize = game.getStockSize();

            // ---- Stock ----
            if (stockSize > 0) {
                int layers = Math.min(3, (stockSize + 7) / 8); // 1..3 cards peeking out
                drawShadow(g2, s.x + (layers - 1) * 2, s.y + (layers - 1) * 2);
                for (int i = layers - 1; i >= 0; i--) {
                    card.drawBack(g2, s.x + i * 2, s.y + i * 2);
                }
                if (hoverStock) {
                    drawGlow(g2, s.x, s.y);
                }

                // Count badge in the top-right corner
                int d = 28;
                int bx = s.x + CW - d / 2 - 4;
                int by = s.y - d / 2 + 4;
                g2.setColor(GOLD);
                g2.fillOval(bx, by, d, d);
                g2.setColor(new Color(40, 30, 5));
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                drawCentered(g2, String.valueOf(stockSize), new Rectangle(bx, by, d, d));
            } else {
                drawEmptySlot(g2, s.x, s.y);
            }

            // ---- Waste ----
            card wasteCard = game.getWasteCard();
            if (wasteCard != null && !wasteCard.isRemoved()) {
                drawShadow(g2, w.x, w.y);
                wasteCard.setSelected(wasteSelected);
                wasteCard.draw(g2, w.x, w.y);
                if (hoverWaste && !wasteSelected) {
                    drawGlow(g2, w.x, w.y);
                }
            } else {
                drawEmptySlot(g2, w.x, w.y);
            }

            // ---- Labels ----
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(TEXT_MUTED);
            drawCentered(g2, "STOCK", new Rectangle(s.x, s.y + CH + 10, CW, 20));
            drawCentered(g2, "WASTE", new Rectangle(w.x, w.y + CH + 10, CW, 20));
        }

        /** Semi-transparent overlay with the win/lose message. */
        private void drawGameOverOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            int bw = 380, bh = 170;
            int bx = (WINDOW_WIDTH - bw) / 2;
            int by = (WINDOW_HEIGHT - bh) / 2 - 20;

            g2.setColor(new Color(250, 247, 238));
            g2.fillRoundRect(bx, by, bw, bh, 24, 24);
            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(bx, by, bw, bh, 24, 24);

            g2.setFont(new Font("Serif", Font.BOLD, 28));
            g2.setColor(new Color(20, 70, 40));
            drawCentered(g2, gameOverTitle, new Rectangle(bx, by + 28, bw, 40));

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.setColor(new Color(90, 90, 90));
            drawCentered(g2, gameOverSubtitle, new Rectangle(bx, by + 78, bw, 24));

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(new Color(150, 115, 30));
            drawCentered(g2, "Click anywhere to deal a new game", new Rectangle(bx, by + 115, bw, 24));
        }

        // ---- Small drawing helpers ----

        private void drawShadow(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(x + 2, y + 4, CW, CH, ARC, ARC);
        }

        private void drawGlow(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(255, 255, 255, 170));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(x - 1, y - 1, CW + 1, CH + 1, ARC + 2, ARC + 2);
        }

        /** Dashed outline for an empty pile. */
        private void drawEmptySlot(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(x, y, CW, CH, ARC, ARC);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[] {6f, 5f}, 0f));
            g2.drawRoundRect(x, y, CW - 1, CH - 1, ARC, ARC);
        }

        /** Draws text centred inside a rectangle using the current font. */
        private void drawCentered(Graphics2D g2, String text, Rectangle r) {
            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
            int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
        }

        // =====================================================================
        // Mouse handling
        // =====================================================================

        /**
         * @return {row, col} of the topmost visible pyramid card under (mx,my),
         *         or null.  Rows overlap, so we search from the bottom row up.
         */
        private int[] findPyramidCardAt(int mx, int my) {
            for (int r = PyramidSolitaireGame.ROWS - 1; r >= 0; r--) {
                for (int c = 0; c <= r; c++) {
                    card cd = game.getPyramidCard(r, c);
                    if (cd == null || cd.isRemoved()) {
                        continue;
                    }
                    if (new Rectangle(cardX(r, c), cardY(r), CW, CH).contains(mx, my)) {
                        return new int[] {r, c};
                    }
                }
            }
            return null;
        }

        /**
         * Updates hover state and the mouse cursor.
         * @return true if anything changed (so the panel needs repainting).
         */
        private boolean updateHover(int mx, int my) {
            int oldRow = hoverRow, oldCol = hoverCol;
            boolean oldStock = hoverStock, oldWaste = hoverWaste, oldButton = hoverButton;

            hoverRow = hoverCol = -1;
            hoverStock = hoverWaste = hoverButton = false;

            if (gameOverTitle == null) {
                hoverButton = NEW_GAME_BUTTON.contains(mx, my);
                hoverStock = game.getStockSize() > 0 && stockRect().contains(mx, my);
                hoverWaste = game.getWasteCard() != null && wasteRect().contains(mx, my);
                int[] hit = findPyramidCardAt(mx, my);
                if (hit != null && game.isFree(hit[0], hit[1])) {
                    hoverRow = hit[0];
                    hoverCol = hit[1];
                }
            }

            boolean clickable = gameOverTitle != null || hoverButton || hoverStock
                    || hoverWaste || hoverRow != -1;
            setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

            return oldRow != hoverRow || oldCol != hoverCol || oldStock != hoverStock
                    || oldWaste != hoverWaste || oldButton != hoverButton;
        }

        /** Handle a click at (mx,my). */
        private void handleClick(int mx, int my) {
            // Game-over overlay: any click deals a new game
            if (gameOverTitle != null) {
                resetGame();
                return;
            }

            // New Game button
            if (NEW_GAME_BUTTON.contains(mx, my)) {
                resetGame();
                return;
            }

            // Stock pile -> draw a card
            if (stockRect().contains(mx, my)) {
                if (game.getStockSize() > 0) {
                    clearSelection();
                    game.drawFromStock();
                    checkWinOrLoss();
                }
                return;
            }

            // Waste pile -> toggle selection
            if (wasteRect().contains(mx, my)) {
                if (game.getWasteCard() != null) {
                    if (wasteSelected) {
                        wasteSelected = false;
                    } else {
                        clearSelection();
                        wasteSelected = true;
                    }
                }
                return;
            }

            // Pyramid card
            int[] hit = findPyramidCardAt(mx, my);
            if (hit != null) {
                attemptSelectOrRemovePyramid(hit[0], hit[1]);
                return;
            }

            // Anywhere else -> clear selection
            clearSelection();
        }

        /** Deselect any pyramid card and the waste. */
        private void clearSelection() {
            if (selRow != -1 && selCol != -1) {
                card prev = game.getPyramidCard(selRow, selCol);
                if (prev != null) prev.setSelected(false);
            }
            selRow = -1;
            selCol = -1;
            wasteSelected = false;
        }

        /**
         * Attempt to select or remove the pyramid card at (row,col):
         *   A) Free King -> remove immediately.
         *   B) Waste selected -> try to pair with waste (sum = 13).
         *   C) Another pyramid card selected -> try to pair both (sum = 13).
         *   D) Otherwise -> select this card.
         */
        private void attemptSelectOrRemovePyramid(int row, int col) {
            card clicked = game.getPyramidCard(row, col);
            if (clicked == null || clicked.isRemoved() || !game.isFree(row, col)) {
                return;
            }

            // A) Free King
            if (clicked.isKing()) {
                clearSelection();
                game.removeKing(row, col);
                checkWinOrLoss();
                return;
            }

            // B) Waste selected
            if (wasteSelected) {
                if (game.canPairWithWaste(row, col)) {
                    game.removeWithWaste(row, col);
                    clearSelection();
                    checkWinOrLoss();
                } else {
                    clearSelection();
                    selRow = row;
                    selCol = col;
                    clicked.setSelected(true);
                }
                return;
            }

            // C) Another pyramid card selected
            if (selRow != -1 && selCol != -1) {
                if (selRow == row && selCol == col) {
                    clearSelection();
                    return;
                }
                if (game.canPair(selRow, selCol, row, col)) {
                    game.removePair(selRow, selCol, row, col);
                    clearSelection();
                    checkWinOrLoss();
                } else {
                    card prev = game.getPyramidCard(selRow, selCol);
                    if (prev != null) prev.setSelected(false);
                    selRow = row;
                    selCol = col;
                    clicked.setSelected(true);
                }
                return;
            }

            // D) Select this card
            selRow = row;
            selCol = col;
            clicked.setSelected(true);
        }

        // =====================================================================
        // Game state checks
        // =====================================================================

        /** @return how many pyramid cards are still in play. */
        private int countPyramidLeft() {
            int count = 0;
            for (int r = 0; r < PyramidSolitaireGame.ROWS; r++) {
                for (int c = 0; c <= r; c++) {
                    card cd = game.getPyramidCard(r, c);
                    if (cd != null && !cd.isRemoved()) count++;
                }
            }
            return count;
        }

        /**
         * @return true if the player can still do something: draw from stock,
         *         remove a free King, pair two free cards, or pair a free card
         *         with the waste.
         */
        private boolean hasAnyMove() {
            if (game.getStockSize() > 0) {
                return true;
            }
            for (int r1 = 0; r1 < PyramidSolitaireGame.ROWS; r1++) {
                for (int c1 = 0; c1 <= r1; c1++) {
                    if (!game.isFree(r1, c1)) continue;

                    if (game.getPyramidCard(r1, c1).isKing()) return true;
                    if (game.canPairWithWaste(r1, c1)) return true;

                    for (int r2 = r1; r2 < PyramidSolitaireGame.ROWS; r2++) {
                        int startC = (r2 == r1 ? c1 + 1 : 0);
                        for (int c2 = startC; c2 <= r2; c2++) {
                            if (game.canPair(r1, c1, r2, c2)) return true;
                        }
                    }
                }
            }
            return false;
        }

        /** After each action, show the win/lose overlay if the game is over. */
        private void checkWinOrLoss() {
            if (game.isPyramidEmpty()) {
                clearSelection();
                gameOverTitle = "You cleared it!";
                gameOverSubtitle = "Every card in the pyramid is gone.";
                return;
            }
            if (!hasAnyMove()) {
                clearSelection();
                int left = countPyramidLeft();
                gameOverTitle = "No more moves";
                gameOverSubtitle = left + (left == 1 ? " card" : " cards") + " left in the pyramid.";
            }
        }

        /** Deals a fresh game and clears selections and the overlay. */
        private void resetGame() {
            game.dealNewGame();
            clearSelection();
            gameOverTitle = null;
            gameOverSubtitle = null;
        }
    }

    /** Main entry point: launch the GUI on the Event Dispatch Thread. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PyramidSolitaireGUI::new);
    }
}
