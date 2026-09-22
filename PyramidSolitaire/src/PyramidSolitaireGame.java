import java.util.ArrayList;
import java.util.List;

/**
 * The PyramidSolitaireGame class encapsulates the core game state and logic:
 *   • A 7‐row pyramid (28 cards total)
 *   • A face‐down stock pile (remaining cards)
 *   • A face‐up waste pile (top card only)
 *   • Methods for checking which cards are free, removing pairs, drawing from stock, etc.
 */
public class PyramidSolitaireGame {
    public static final int ROWS = 7;          // number of rows in the pyramid
    public static final int PYRAMID_CARDS = 28; // total cards in a 7‐row pyramid

    // 2D array representing the pyramid: row 0 has 1 card, row 1 has 2, …, row 6 has 7 cards
    private final card[][] pyramid;

    // The remaining cards after dealing the pyramid form the face-down stock:
    private final List<card> stock;

    // The waste is the single most recently drawn card (face‐up). Null if no waste.
    private card waste;

    /** Constructor: initialize data structures and deal a new shuffled game. */
    public PyramidSolitaireGame() {
        // Initialize the 2D pyramid array: row r has (r+1) slots
        pyramid = new card[ROWS][];
        for (int r = 0; r < ROWS; r++) {
            pyramid[r] = new card[r + 1];
        }

        // Initialize stock as an empty ArrayList
        stock = new ArrayList<>();

        // Deal cards into pyramid and stock
        waste = null;
        dealNewGame();
    }

    /** Deals a brand‐new shuffled deck into the pyramid and stock piles. */
    public void dealNewGame() {
        // 1) Create and shuffle a new Deck
        deck deck = new deck();

        // 2) Deal the top 28 cards into the 7‐row pyramid
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= r; c++) {
                pyramid[r][c] = deck.dealOne();
            }
        }

        // 3) Remaining cards (24 total) go into stock (face‐down), preserving order
        stock.clear();
        while (deck.hasNext()) {
            stock.add(deck.dealOne());
        }

        // 4) No waste initially
        waste = null;
    }

    /**
     * @param row  pyramid row index (0..6)
     * @param col  pyramid column index (0..row)
     * @return the Card object at pyramid[row][col], or null if indices out of range.
     */
    public card getPyramidCard(int row, int col) {
        if (row < 0 || row >= ROWS) return null;
        if (col < 0 || col > row) return null;
        return pyramid[row][col];
    }

    /**
     * @return true if the entire pyramid has been cleared (all 28 cards removed).
     */
    public boolean isPyramidEmpty() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= r; c++) {
                card card = pyramid[r][c];
                if (card != null && !card.isRemoved()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines if the card at (row,col) is “free” (i.e., no cards cover it).
     * A card is covered if there exists a non‐removed card at either (row+1, col) or (row+1, col+1).
     * Bottom‐row cards (row==6) are free by default if not removed.
     *
     * @param row  pyramid row index
     * @param col  pyramid column index
     * @return true if the card is not removed and both cards directly beneath it are removed (or out‐of‐bounds).
     */
    public boolean isFree(int row, int col) {
        card card = getPyramidCard(row, col);
        if (card == null || card.isRemoved()) {
            return false; // either no card or already removed
        }
        if (row == ROWS - 1) {
            // bottom row: automatically free if not removed
            return true;
        }
        // Check cards immediately below-left and below-right
        card belowLeft = getPyramidCard(row + 1, col);
        card belowRight = getPyramidCard(row + 1, col + 1);

        boolean leftRemoved = (belowLeft == null || belowLeft.isRemoved());
        boolean rightRemoved = (belowRight == null || belowRight.isRemoved());

        return leftRemoved && rightRemoved;
    }

    /** @return the current face-up waste card, or null if none. */
    public card getWasteCard() {
        return waste;
    }

    /** @return the number of cards still in the face-down stock pile. */
    public int getStockSize() {
        return stock.size();
    }

    /**
     * Draw one card from the back of the stock.  The card is removed from stock
     * and becomes the new waste (face-up).  If stock is empty, does nothing.
     */
    public void drawFromStock() {
        if (stock.isEmpty()) {
            return;
        }
        waste = stock.remove(stock.size() - 1);
    }

    /**
     * @param row  pyramid row index
     * @param col  pyramid column index
     * @return true if the free pyramid card at (row,col) can be paired (sum=13) with the waste.
     */
    public boolean canPairWithWaste(int row, int col) {
        card p = getPyramidCard(row, col);
        if (!isFree(row, col) || p == null) {
            return false;
        }
        if (waste == null) {
            return false;
        }
        return (p.getValue() + waste.getValue() == 13);
    }

    /**
     * @param r1  row index of first pyramid card
     * @param c1  column index of first pyramid card
     * @param r2  row index of second pyramid card
     * @param c2  column index of second pyramid card
     * @return true if both cards are free, not the same card, and sum to 13
     */
    public boolean canPair(int r1, int c1, int r2, int c2) {
        // Must be two distinct positions
        if (r1 == r2 && c1 == c2) return false;

        // Both cards must be free
        if (!isFree(r1, c1) || !isFree(r2, c2)) return false;

        card cA = getPyramidCard(r1, c1);
        card cB = getPyramidCard(r2, c2);
        if (cA == null || cB == null) return false;

        return (cA.getValue() + cB.getValue() == 13);
    }

    /**
     * If the free card at (row,col) is a King (value=13), remove it alone.
     * @param row  pyramid row index
     * @param col  pyramid column index
     * @return true if removed successfully; false if not a free King.
     */
    public boolean removeKing(int row, int col) {
        card c = getPyramidCard(row, col);
        if (c != null && isFree(row, col) && c.isKing()) {
            c.setRemoved(true);
            return true;
        }
        return false;
    }

    /**
     * Removes two free pyramid cards at (r1,c1) and (r2,c2) if they sum to 13.
     * @return true if removal succeeded, false otherwise.
     */
    public boolean removePair(int r1, int c1, int r2, int c2) {
        if (canPair(r1, c1, r2, c2)) {
            getPyramidCard(r1, c1).setRemoved(true);
            getPyramidCard(r2, c2).setRemoved(true);
            return true;
        }
        return false;
    }

    /**
     * Removes the free pyramid card at (row,col) and the waste if they sum to 13.
     * @param row  pyramid row index
     * @param col  pyramid column index
     * @return true if removal succeeded; false otherwise.
     */
    public boolean removeWithWaste(int row, int col) {
        if (canPairWithWaste(row, col)) {
            getPyramidCard(row, col).setRemoved(true);
            waste = null;  // clear the waste
            return true;
        }
        return false;
    }
}