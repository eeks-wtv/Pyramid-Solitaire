# Pyramid Solitaire

## About:

A desktop implementation of Pyramid Solitaire made with Java and Swing. Cards are dealt into a 7-row pyramid, and the player removes free cards (or pairs with the waste pile) whose values sum to 13, working to clear the entire pyramid before running out of moves.

## Technical Details

### Key Features:

**Pyramid Layout:**

- 7-row, 28-card pyramid dealt from a shuffled 52-card deck
- Cards are only playable once "free" (not covered by any card below them)

**Stock & Waste Piles:**
- Draw cards one at a time from the face-down stock
- Waste pile shows the most recently drawn card, available for pairing

**Card Matching:**
- Select any two free cards that sum to 13 to remove them
- Match a free pyramid card with the waste card to remove both
- Kings (value 13) can be removed on their own

**Game State Handling:**
- Win detection when the pyramid is fully cleared
- Loss detection when no more valid moves remain
- Automatically deals a new game after a win or loss

### Platform:

Language: Java

GUI Framework: Swing
