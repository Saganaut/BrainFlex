import { useGetDeckQuery } from "@/store/BrainFlexApi";
import { getRouteApi } from "@tanstack/react-router";
import styles from "./CreateDashboard.module.css";
import { Loader } from "../Common/Loader/Loader";
import { McqGraphic } from "../Common/Slides/SlideTypeGraphics/McqGraphic";
const routeApi = getRouteApi("/decks/$deckId/view");

const SlideDisplay = () => {
  const { deckId } = routeApi.useParams();
  const { questionId } = routeApi.useSearch();

  const { element, isLoading } = useGetDeckQuery(
    { id: deckId },
    {
      selectFromResult: ({ data, isLoading }) => ({
        isLoading,
        element: data?.elements?.find((e) => e.id === questionId),
      }),
    },
  );
  console.log("element", element);
  return (
    <div className={styles.slideDisplay}>
      <Loader />
      {isLoading ? <Loader /> : <McqGraphic />}
    </div>
  );
};

export { SlideDisplay };
