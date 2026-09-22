import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;

/**
 * The PyramidSolitaireGUI class provides a Swing‐based graphical interface
 * for playing Pyramid Solitaire.  It draws the pyramid, the face-down stock,
 * and the face-up waste.  Mouse clicks allow the user to:
 *   • Draw from stock → waste
 *   • Select/deselect the waste
 *   • Select two free pyramid cards to remove (sum=13)
 *   • Select a free pyramid King to remove alone
 *   • Pair a free pyramid card with the waste to remove (sum=13)
 *
 * After each removal or draw, it checks for win/loss conditions and resets
 * if the game ends.
 */
public class PyramidSolitaireGUI extends JFrame {
    private static final int WINDOW_WIDTH = 600;    // frame width in pixels
    private static final int WINDOW_HEIGHT = 900;   // frame height in pixels

    // Horizontal and vertical gaps between cards in the pyramid
    private static final int X_GAP = 80;
    private static final int Y_GAP = 100;

    // Top margin for drawing the pyramid
    private static final int START_Y = 20;

    private final PyramidSolitaireGame game; // the game logic/state
    private final GamePanel panel;           // custom JPanel for drawing

    // Selection tracking:
    // If a pyramid card is selected, these hold its row/col; otherwise -1/-1.
    private int selRow = -1, selCol = -1;
    // If the waste card is selected, this is true; otherwise false.
    private boolean wasteSelected = false;

    /** Constructor: set up frame properties, instantiate game, and show GUI. */
    public PyramidSolitaireGUI() {
        super("Pyramid Solitaire");

        game = new PyramidSolitaireGame();  // create new game state

        panel = new GamePanel();            // create drawing panel
        setContentPane(panel);

        // Standard JFrame setup
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();                          // size the frame to match panel’s preferred size
        setLocationRelativeTo(null);     // center on screen
        setVisible(true);
    }

    /**
     * Inner class GamePanel extends JPanel to perform custom drawing
     * and mouse handling for Pyramid Solitaire.
     */
    private class GamePanel extends JPanel {
        /** Constructor: set preferred size, background, and add mouse listener. */
        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setBackground(new Color(0, 120, 0)); // dark green “felt” background

            // Add a MouseAdapter to capture mouse clicks and handle appropriately
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Delegate to our custom handler, then repaint
                    handleClick(e.getX(), e.getY());
                    repaint();
                }
            });
        }

        /** paintComponent is called whenever the panel must be redrawn. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawPyramid(g);
            drawStockAndWaste(g);
        }

        /**
         * Draws all the cards in the pyramid based on game state.
         * Only non-removed cards are drawn (face-up).
         * If a card matches selRow/selCol (and waste not selected), it is drawn highlighted.
         */
        private void drawPyramid(Graphics g) {
            // Center horizontally: pyramid’s total width depends on row length
            int centerX = WINDOW_WIDTH / 2;

            for (int r = 0; r < PyramidSolitaireGame.ROWS; r++) {
                int cardsInRow = r + 1;
                // Compute total pixel width of this row:
                int rowWidth = (cardsInRow - 1) * X_GAP + card.CARD_WIDTH;
                // Start X so that row is centered
                int startX = centerX - (rowWidth / 2);
                // Y coordinate for this row
                int y = START_Y + (r * Y_GAP);

                for (int c = 0; c <= r; c++) {
                    card card = game.getPyramidCard(r, c);
                    if (card == null || card.isRemoved()) {
                        // If no card or it’s been removed, skip drawing
                        continue;
                    }

                    // If this card is currently selected (and waste is not selected),
                    // mark it so that card.draw(...) shows highlighted icon.
                    if (selRow == r && selCol == c && !wasteSelected) {
                        card.setSelected(true);
                    } else {
                        card.setSelected(false);
                    }

                    // Compute the X position of column c in row r
                    int x = startX + (c * X_GAP);
                    card.draw(g, x, y);
                }
            }
        }

        /**
         * Draws the face‐down stock pile (back‐of‐card) and the face‐up waste pile.
         * Also draws labels for STOCK (with remaining count) and WASTE.
         */
        private void drawStockAndWaste(Graphics g) {
            // Coordinates for stock pile:
            int stockX = 50;
            int stockY = START_Y + (PyramidSolitaireGame.ROWS * Y_GAP) + 20;
            Rectangle stockRect = new Rectangle(stockX, stockY, card.CARD_WIDTH, card.CARD_HEIGHT);

            // If there are cards left in stock, draw a back‐of‐card; otherwise gray box
            if (game.getStockSize() > 0) {
                card.drawBack(g, stockX, stockY);
            } else {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(stockX, stockY, card.CARD_WIDTH, card.CARD_HEIGHT);
            }

            // Coordinates for waste pile (right of stock):
            int wasteX = 170;
            int wasteY = stockY;
            Rectangle wasteRect = new Rectangle(wasteX, wasteY, card.CARD_WIDTH, card.CARD_HEIGHT);

            card wasteCard = game.getWasteCard();
            if (wasteCard != null && !wasteCard.isRemoved()) {
                // If the waste card is currently selected, mark it selected
                wasteCard.setSelected(wasteSelected);
                wasteCard.draw(g, wasteX, wasteY);
            } else {
                // If no waste card, draw a light gray placeholder
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(wasteX, wasteY, card.CARD_WIDTH, card.CARD_HEIGHT);
            }

            // Draw text labels for STOCK and WASTE
            g.setColor(Color.WHITE);
            g.drawString("STOCK (" + game.getStockSize() + ")", stockX, stockY - 5);
            g.drawString("WASTE", wasteX, wasteY - 5);
        }

        /**
         * Handle a mouse click at screen coordinates (mx, my).
         * Determine whether the user clicked:
         *   1) Stock pile → draw from stock.
         *   2) Waste pile → toggle waste selection.
         *   3) Pyramid card → select/remove logic.
         *   4) Elsewhere → clear any current selection.
         *
         * @param mx  x‐coordinate of mouse click
         * @param my  y‐coordinate of mouse click
         */
        private void handleClick(int mx, int my) {
            // 1) Check if click was on Stock pile
            int stockX = 50;
            int stockY = START_Y + (PyramidSolitaireGame.ROWS * Y_GAP) + 20;
            Rectangle stockRect = new Rectangle(stockX, stockY, card.CARD_WIDTH, card.CARD_HEIGHT);
            if (stockRect.contains(mx, my)) {
                // If there are cards in stock, draw one to waste and clear selections
                if (game.getStockSize() > 0) {
                    clearSelection();
                    game.drawFromStock();
                }
                return;
            }

            // 2) Check if click was on Waste pile
            int wasteX = 170;
            int wasteY = stockY;
            Rectangle wasteRect = new Rectangle(wasteX, wasteY, card.CARD_WIDTH, card.CARD_HEIGHT);
            if (wasteRect.contains(mx, my)) {
                card w = game.getWasteCard();
                if (w != null) {
                    // Toggle selection on the waste card
                    if (wasteSelected) {
                        wasteSelected = false;
                    } else {
                        clearSelection();  // deselect any pyramid card first
                        wasteSelected = true;
                    }
                }
                return;
            }

            // 3) Check if click was on any Pyramid card
            int centerX = WINDOW_WIDTH / 2;
            for (int r = 0; r < PyramidSolitaireGame.ROWS; r++) {
                int cardsInRow = r + 1;
                int rowWidth = (cardsInRow - 1) * X_GAP + card.CARD_WIDTH;
                int startX = centerX - (rowWidth / 2);
                int y = START_Y + (r * Y_GAP);

                for (int c = 0; c <= r; c++) {
                    card card = game.getPyramidCard(r, c);
                    if (card == null || card.isRemoved()) {
                        continue; // skip empty or removed slots
                    }
                    int x = startX + (c * X_GAP);
                    Rectangle cardRect = new Rectangle(x, y, card.CARD_WIDTH, card.CARD_HEIGHT);

                    if (cardRect.contains(mx, my)) {
                        // Click fell on this pyramid card
                        attemptSelectOrRemovePyramid(r, c);
                        return;
                    }
                }
            }

            // 4) Click outside any interactive area → clear all selections
            clearSelection();
        }

        /** Deselect any pyramid card and the waste. */
        private void clearSelection() {
            // If a pyramid card was selected, un‐highlight it
            if (selRow != -1 && selCol != -1) {
                card prev = game.getPyramidCard(selRow, selCol);
                if (prev != null) prev.setSelected(false);
            }
            selRow = -1;
            selCol = -1;
            wasteSelected = false;
        }

        /**
         * Attempt to select or remove the pyramid card at (row,col), following rules:
         *   A) If it’s a free King → remove immediately.
         *   B) If waste is selected, attempt to pair this free card + waste (sum=13).
         *   C) If another pyramid card is selected, attempt to pair both (sum=13).
         *   D) Otherwise, simply select/deselect this card.
         *
         * @param row  pyramid row index
         * @param col  pyramid column index
         */
        private void attemptSelectOrRemovePyramid(int row, int col) {
            card clicked = game.getPyramidCard(row, col);
            if (clicked == null || clicked.isRemoved()) {
                return; // no card or already removed
            }
            if (!game.isFree(row, col)) {
                // Not a free card, so ignore click
                return;
            }

            // A) If it’s a free King, remove it and reset selections
            if (clicked.isKing()) {
                clearSelection();
                game.removeKing(row, col);
                checkWinOrLoss();
                return;
            }

            // B) If the waste card is currently selected, try pairing with waste
            if (wasteSelected) {
                if (game.canPairWithWaste(row, col)) {
                    // Remove both the clicked pyramid card and the waste
                    game.removeWithWaste(row, col);
                    clearSelection();
                    checkWinOrLoss();
                } else {
                    // Cannot pair with waste → select this pyramid card instead
                    clearSelection();
                    selRow = row;
                    selCol = col;
                    clicked.setSelected(true);
                }
                return;
            }

            // C) If another pyramid card is already selected, try pairing those two
            if (selRow != -1 && selCol != -1) {
                // If clicked the same card, unselect it
                if (selRow == row && selCol == col) {
                    clearSelection();
                    return;
                }
                // Attempt to pair (selRow,selCol) with (row,col)
                if (game.canPair(selRow, selCol, row, col)) {
                    game.removePair(selRow, selCol, row, col);
                    clearSelection();
                    checkWinOrLoss();
                } else {
                    // Not a valid pair → switch selection to this new card
                    card prev = game.getPyramidCard(selRow, selCol);
                    if (prev != null) prev.setSelected(false);
                    selRow = row;
                    selCol = col;
                    clicked.setSelected(true);
                }
                return;
            }

            // D) Otherwise, no other card is selected → select/deselect this pyramid card
            selRow = row;
            selCol = col;
            clicked.setSelected(true);
        }

        /**
         * After each action (removal or draw), check if the player has won (pyramid empty)
         * or lost (no valid moves left).  If so, show a dialog and reset the game.
         */
        private void checkWinOrLoss() {
            // WIN: if pyramid is completely cleared
            if (game.isPyramidEmpty()) {
                JOptionPane.showMessageDialog(this, "Congratulations! You cleared the pyramid!");
                resetGame();
                return;
            }

            // LOSS: only possible when stock and waste are both empty AND
            //       there are no valid free‐card pairings or free kings in the pyramid
            if (game.getStockSize() == 0 && game.getWasteCard() == null) {
                boolean moveExists = false;

                // 1) Check for any free King
                outerLoop:
                for (int r = 0; r < PyramidSolitaireGame.ROWS; r++) {
                    for (int c = 0; c <= r; c++) {
                        card card = game.getPyramidCard(r, c);
                        if (card != null && !card.isRemoved() && game.isFree(r, c) && card.isKing()) {
                            moveExists = true;
                            break outerLoop;
                        }
                    }
                }

                // 2) If no free King, check for any two free pyramid cards summing to 13
                if (!moveExists) {
                    outerLoop2:
                    for (int r1 = 0; r1 < PyramidSolitaireGame.ROWS; r1++) {
                        for (int c1 = 0; c1 <= r1; c1++) {
                            if (!game.isFree(r1, c1)) continue;
                            for (int r2 = r1; r2 < PyramidSolitaireGame.ROWS; r2++) {
                                int startC = (r2 == r1 ? c1 + 1 : 0);
                                for (int c2 = startC; c2 <= r2; c2++) {
                                    if (game.canPair(r1, c1, r2, c2)) {
                                        moveExists = true;
                                        break outerLoop2;
                                    }
                                }
                            }
                        }
                    }
                }

                // If still no moves, it’s a loss
                if (!moveExists) {
                    JOptionPane.showMessageDialog(this, "No more valid moves. You lose.");
                    resetGame();
                }
            }
        }

        /** Resets the game by dealing a fresh new pyramid and clearing selections. */
        private void resetGame() {
            game.dealNewGame();
            clearSelection();
        }
    }

    /** Main entry point: launch the GUI on the Event Dispatch Thread. */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new PyramidSolitaireGUI();
        });
    }
}