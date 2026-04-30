import { createFileRoute, useSearch } from "@tanstack/react-router";

type RegisterSearch = {
  googleId?: string;
  email?: string;
  name?: string;
  picture?: string;
};

export const Route = createFileRoute("/register")({
  validateSearch: (search: Record<string, unknown>): RegisterSearch => {
    return {
      googleId: search.googleId as string,
      email: search.email as string,
      name: search.name as string,
      picture: search.picture as string,
    };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const search = useSearch({ from: "/register" });

  console.log("search", search);
  return (
    <div>
      <h1>What shall we call you?</h1>
      <div>
        <form>
          <div>
            <label>Username</label>
            <input></input>
          </div>
          <button type='submit'>Submit</button>
        </form>
      </div>
    </div>
  );
}
