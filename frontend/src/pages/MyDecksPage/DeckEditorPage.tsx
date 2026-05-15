// Placeholder while the dashboard editor is being rebuilt against the new
// polymorphic DeckElement model. The previous question-CRUD UI doesn't fit
// the new shape; once your authoring screens land they replace this stub.
import { Link } from "@tanstack/react-router";

interface DeckEditorPageProps {
  deckId?: string;
  returnTo?: string;
}

const DeckEditorPage = ({ deckId, returnTo }: DeckEditorPageProps) => (
  <div
    style={{
      padding: "2rem",
      display: "flex",
      flexDirection: "column",
      gap: "1rem",
    }}>
    <h1>Deck editor</h1>
    <p>
      The deck editor is being rebuilt to author the new polymorphic deck
      elements (slides + 9 question kinds). Live editing is temporarily on hold.
    </p>
    {deckId && <p>Deck id: {deckId}</p>}
    <Link to={(returnTo ?? "/decks") as "/"}>Back</Link>
  </div>
);

export { DeckEditorPage };
