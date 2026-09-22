import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Deck class builds, shuffles, and deals a standard 52‐card deck.
 * It is used by PyramidSolitaireGame to fill the pyramid (28 cards)
 * and the stock pile (remaining cards).
 */
public class deck {
    private final List<card> cards;   // underlying list representing all 52 cards
    private int index;                // next card to deal (0..51)

    /** Constructor: create 52 Card objects (all ranks × suits), shuffle them, reset index. */
    public deck() {
        cards = new ArrayList<>(52);

        // Build 52 cards: for each Suit and for each Rank, create a new Card
        for (card.Suit s : card.Suit.values()) {
            for (card.Rank r : card.Rank.values()) {
                cards.add(new card(r, s));
            }
        }

        shuffle();  // randomize their order and reset index to 0
        index = 0;
    }

    /** Randomly shuffle the deck and reset deal‐index to 0. */
    public void shuffle() {
        Collections.shuffle(cards);
        index = 0;
    }

    /** @return true if there are still cards left to be dealt. */
    public boolean hasNext() {
        return index < cards.size();
    }

    /**
     * Deals and returns the next Card in the shuffled sequence.
     * If no cards remain, returns null.
     */
    public card dealOne() {
        if (!hasNext()) {
            return null;
        }
        return cards.get(index++);
    }

    /**
     * @return the number of cards remaining in the deck that haven't been dealt yet.
     */
    public int cardsRemaining() {
        return cards.size() - index;
    }
}