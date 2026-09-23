import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * The Card class represents a single playing card in Pyramid Solitaire.
 * It handles the rank/suit, image-loading, selection state, and whether the
 * card has been removed.  It also knows how to draw itself (face-up or back).
 *
 * File-naming conventions (under an "images/" folder at runtime):
 *   - Face-up:      "<rank>_of_<suit>.png"   e.g. "8_of_spades.png", "queen_of_hearts.png"
 *   - Face-down:    "cardback.png"
 *
 * rank: ace, 2, 3, 4, 5, 6, 7, 8, 9, 10, jack, queen, king
 * suit: clubs, diamonds, hearts, spades
 *
 * Cards are drawn with rounded corners and a thin border.  A selected card
 * gets a translucent gold overlay and a gold outline.
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

        /** @return the numeric pip value (1-13) of this Rank. */
        public int getValue() { return value; }

        @Override public String toString() { return name; }
    }

    // Size (in pixels) that every card is drawn at
    public static final int CARD_WIDTH = 88;
    public static final int CARD_HEIGHT = 128;

    // Roundness of the card corners (also used by the GUI for shadows/overlays)
    public static final int CORNER_ARC = 12;

    // Folder and file extension for the card images
    private static final String IMAGE_DIR = "images/";
    private static final String EXT = ".png";

    // Colours for the border and selection highlight
    private static final Color BORDER = new Color(0, 0, 0, 90);
    private static final Color HIGHLIGHT_FILL = new Color(255, 215, 80, 80);
    private static final Color HIGHLIGHT_BORDER = new Color(255, 200, 40);

    private final Rank rank;       // this card's rank (ACE-KING)
    private final Suit suit;       // this card's suit
    private final Image faceImage; // face-up image, already scaled to card size
    private boolean selected;      // is this card currently "selected"?
    private boolean removed;       // has this card been removed from play?

    /** Shared back-of-card image; used when drawing the face-down stock. */
    private static final Image BACK_IMAGE = loadScaled(IMAGE_DIR + "cardback" + EXT);

    /**
     * Constructor: load the face image based on rank/suit.
     * Example: rank=EIGHT, suit=SPADES loads "images/8_of_spades.png".
     * Filenames are case-sensitive on Unix/macOS.
     */
    public card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.selected = false;
        this.removed = false;

        String fileName = rank.toString() + "_of_" + suit.toString() + EXT;
        this.faceImage = loadScaled(IMAGE_DIR + fileName);
    }

    /**
     * Loads an image from disk and scales it to CARD_WIDTH x CARD_HEIGHT once,
     * so it doesn't have to be rescaled every repaint.
     * @return the scaled image, or null if the file couldn't be loaded.
     */
    private static Image loadScaled(String path) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("Could not load card image: " + path);
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(CARD_WIDTH, CARD_HEIGHT, Image.SCALE_SMOOTH);
        // Wrap in an ImageIcon to force the scaled image to finish loading now
        return new ImageIcon(scaled).getImage();
    }

    /** @return the numeric pip value (1..13) of this card. */
    public int getValue() {
        return rank.getValue();
    }

    /**
     * Mark this card as "removed" (no longer drawn or clickable).
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

    /** Set or clear this card's "selected" highlight state. */
    public void setSelected(boolean sel) {
        this.selected = sel;
    }

    /** @return true if this card is currently selected. */
    public boolean isSelected() {
        return selected;
    }

    /** @return a Rectangle representing the on-screen bounds of this card at (x,y). */
    public Rectangle getBounds(int x, int y) {
        return new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * Draws this card face-up at (x,y).  If removed, does nothing.
     * If selected, draws a gold highlight on top of the card.
     */
    public void draw(Graphics g, int x, int y) {
        if (removed) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintRounded(g2, faceImage, x, y, rank + " " + suit);

        if (selected) {
            g2.setColor(HIGHLIGHT_FILL);
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_ARC, CORNER_ARC);
            g2.setColor(HIGHLIGHT_BORDER);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(x + 1, y + 1, CARD_WIDTH - 3, CARD_HEIGHT - 3, CORNER_ARC, CORNER_ARC);
        }
        g2.dispose();
    }

    /** @return true if this card is a King (value == 13). */
    public boolean isKing() {
        return rank == Rank.KING;
    }

    /** Draws the back side of a face-down card at (x,y).  Used for the stock pile. */
    public static void drawBack(Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintRounded(g2, BACK_IMAGE, x, y, "back");
        g2.dispose();
    }

    /**
     * Draws an image clipped to a rounded rectangle with a thin border.
     * If the image is missing, draws a white placeholder with a label instead.
     */
    private static void paintRounded(Graphics2D g2, Image img, int x, int y, String label) {
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_ARC, CORNER_ARC));
        if (img != null) {
            g2.drawImage(img, x, y, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
            g2.setColor(Color.BLACK);
            g2.drawString(label, x + 6, y + CARD_HEIGHT / 2);
        }
        g2.setClip(oldClip);

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, CARD_WIDTH - 1, CARD_HEIGHT - 1, CORNER_ARC, CORNER_ARC);
    }
}
