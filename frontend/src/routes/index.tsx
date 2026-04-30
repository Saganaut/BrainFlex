import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  component: Index,
});

function Index() {
  console.log("HI!");
  return (
    <div className='p-2'>
      <h3>Welcome to BrainFlex!</h3>
    </div>
  );
}
