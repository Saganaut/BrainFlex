import { createFileRoute, useSearch } from "@tanstack/react-router";
import {
  RegistrationForm,
  type RegisterSearch,
} from "../components/Forms/RegistrationForm";

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
      <RegistrationForm registerSearchParams={search} />
    </div>
  );
}
