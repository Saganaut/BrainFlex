import { createFileRoute, useSearch } from "@tanstack/react-router";
import {
  RegistrationForm,
  type RegisterSearch,
} from "../components/Forms/RegistrationForm";

export const Route = createFileRoute("/register")({
  validateSearch: (search: Record<string, unknown>): RegisterSearch => {
    return {
      googleId: typeof search.googleId === "string" ? search.googleId : undefined,
      email: typeof search.email === "string" ? search.email : undefined,
      name: typeof search.name === "string" ? search.name : undefined,
      picture: typeof search.picture === "string" ? search.picture : undefined,
      returnUrl: typeof search.returnUrl === "string" ? search.returnUrl : undefined,
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
