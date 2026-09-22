import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * The Card class represents a single playing card in Pyramid Solitaire.
 * It handles the rank/suit, image‐loading, selection state, and whether the
 * card has been removed.  It also knows how to draw itself (face‐up or back).
 *
 * File‐naming conventions (under an "images/" folder at runtime):
 *   • Face‐up normal:      "<rank><suit>.GIF"
 *   • Face‐up selected:    "<rank><suit>S.GIF"
 *   • Face‐down (back):     "CardBack.GIF"
 *
 * rank ∈ { ace, 2, 3, 4, 5, 6, 7, 8, 9, 10, jack, queen, king }
 * suit ∈ { clubs, diamonds, hearts, spades }
 */
public class card {
    // Enumerations for Suit and Rank, with string codes used for file names:
    public enum Suit {
        CLUBS("clubs"),
        DIAMONDS("diamonds"),
        HEARTS("hearts"),
        SPADES("spades");

        private final String name;
        Suit(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    public enum Rank {
        ACE(1,   "ace"),
        TWO(2,   "2"),
        THREE(3, "3"),
        FOUR(4,  "4"),
        FIVE(5,  "5"),
        SIX(6,   "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9,  "9"),
        TEN(10,  "10"),
        JACK(11, "jack"),
        QUEEN(12,"queen"),
        KING(13, "king");

        private final int value;     // numeric value for pairing logic
        private final String name;   // string used in file names

        Rank(int value, String name) {
            this.value = value;
            this.name = name;
        }

        /** @return the numeric pip value (1–13) of this Rank. */
        public int getValue() { return value; }

        @Override public String toString() { return name; }
    }

    // Fixed width/height (in pixels) for any card image
    public static final int CARD_WIDTH = 73;
    public static final int CARD_HEIGHT = 97;

    private final Rank rank;             // this card’s rank (ACE–KING)
    private final Suit suit;             // this card’s suit (clubs/diamonds/hearts/spades)
    private final ImageIcon faceIcon;    // image for face‐up normal display
    private final ImageIcon highlightedIcon; // image for face‐up highlighted (selected) display
    private boolean selected;            // is this card currently “selected”?
    private boolean removed;             // has this card been removed from play?

    /** Shared, static back‐of‐card icon; used when drawing a face‐down card. */
    private static final ImageIcon BACK_ICON = new ImageIcon("images/CardBack.GIF");

    /**
     * Constructor: load both normal and highlighted images based on rank/suit.
     * Example: if rank=ACE and suit=HEARTS, loads "images/acehearts.GIF" and
     * "images/aceheartsS.GIF".  All filenames are case-sensitive on Unix/macOS.
     *
     * @param rank  the Rank enumeration (ACE, TWO, …, KING)
     * @param suit  the Suit enumeration (CLUBS, DIAMONDS, HEARTS, SPADES)
     */
    public card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.selected = false;
        this.removed = false;

        // Build the base filename: e.g. "acehearts", "10spades", "jackdiamonds", etc.
        String baseName = rank.toString() + suit.toString();

        // Load the normal face‐up icon:
        this.faceIcon = new ImageIcon("images/" + baseName + ".GIF");

        // Load the highlighted‐face icon (when selected):
        this.highlightedIcon = new ImageIcon("images/" + baseName + "S.GIF");
    }

    /** @return the numeric pip value (1..13) of this card. */
    public int getValue() {
        return rank.getValue();
    }

    /**
     * Mark this card as “removed” (no longer drawn or clickable).
     * If removed, it automatically deselects itself.
     */
    public void setRemoved(boolean rem) {
        this.removed = rem;
        if (rem) {
            this.selected = false;
        }
    }

    /** @return true if this card has been removed from the pyramid. */
    public boolean isRemoved() {
        return removed;
    }

    /** Set or clear this card’s “selected” highlight state. */
    public void setSelected(boolean sel) {
        this.selected = sel;
    }

    /** @return true if this card is currently selected. */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param x  the x‐coordinate (left) where this card is drawn
     * @param y  the y‐coordinate (top) where this card is drawn
     * @return a Rectangle representing the on-screen bounds of this card
     */
    public Rectangle getBounds(int x, int y) {
        return new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * Draws this card face-up at the given (x,y).  If removed, does nothing.
     * If selected, draws the highlighted version; otherwise draws the normal face.
     *
     * @param g  Graphics context from the JPanel’s paintComponent
     * @param x  x‐coordinate of top-left corner
     * @param y  y‐coordinate of top-left corner
     */
    public void draw(Graphics g, int x, int y) {
        if (removed) {
            // Don’t draw anything if the card’s flagged as removed
            return;
        }
        ImageIcon toDraw = (selected ? highlightedIcon : faceIcon);
        toDraw.paintIcon(null, g, x, y);
    }

    /**
     * @return true if this card is a King (value == 13).  Special rule: free kings can be removed singly.
     */
    public boolean isKing() {
        return rank == Rank.KING;
    }

    /**
     * Draws the back side of a face‐down card at (x,y).  Used to display the stock pile.
     *
     * @param g  Graphics context
     * @param x  x‐coordinate of top-left corner
     * @param y  y‐coordinate of top-left corner
     */
    public static void drawBack(Graphics g, int x, int y) {
        BACK_ICON.paintIcon(null, g, x, y);
    }
}