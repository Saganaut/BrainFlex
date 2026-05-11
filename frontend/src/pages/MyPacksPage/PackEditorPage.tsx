// Placeholder while the dashboard editor is being rebuilt against the new
// polymorphic DeckElement model. The previous question-CRUD UI doesn't fit
// the new shape; once your authoring screens land they replace this stub.
import { Link } from "@tanstack/react-router";

interface PackEditorPageProps {
  packId?: string;
  returnTo?: string;
}

const PackEditorPage = ({ packId, returnTo }: PackEditorPageProps) => (
  <div style={{ padding: "2rem", display: "flex", flexDirection: "column", gap: "1rem" }}>
    <h1>Pack editor</h1>
    <p>
      The pack editor is being rebuilt to author the new polymorphic deck
      elements (slides + 9 question kinds). Live editing is temporarily on hold.
    </p>
    {packId && <p>Pack id: {packId}</p>}
    <Link to={(returnTo ?? "/my-packs") as "/"}>Back</Link>
  </div>
);

export { PackEditorPage };
