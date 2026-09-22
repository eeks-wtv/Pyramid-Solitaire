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

## Timelines and Milestones:

- Week 1: Basic Setup
- Week 2: Build Card and Deck classes
- Week 3: Implement pyramid dealing and stock/waste logic
- Week 4: Implement pairing and removal rules (including Kings)
- Week 5: Build Swing GUI and card rendering
- Week 6: Add mouse interaction and selection handling
- Week 7: Add win/loss detection and testing
