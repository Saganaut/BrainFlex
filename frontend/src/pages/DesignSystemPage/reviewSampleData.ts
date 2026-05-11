// Sample data for the design system's ReviewPanel demo.
// Mirrors the shape the backend emits from GET /api/showcases/{code}/review.
import type { ShowcaseReviewDto } from "../../store/BrainFlexApi";

export const reviewSampleData: ShowcaseReviewDto = {
  showcaseId: "demo-showcase",
  roomCode: "DEMO00",
  endedAt: new Date().toISOString(),
  scoringEnabled: true,
  placements: [
    {
      userId: "u1",
      userName: "Aragorn",
      guest: false,
      finalScore: 750,
      placement: 1,
      correctAnswers: 4,
      totalQuestions: 5,
    },
    {
      userId: "u2",
      userName: "Legolas",
      guest: false,
      finalScore: 500,
      placement: 2,
      correctAnswers: 3,
      totalQuestions: 5,
    },
    {
      userId: "u3",
      userName: "Gimli",
      guest: false,
      finalScore: 350,
      placement: 3,
      correctAnswers: 2,
      totalQuestions: 5,
    },
  ],
  rounds: [
    {
      round: 0,
      questionId: "q1",
      questionType: "MULTIPLE_CHOICE",
      questionText: "Which planet is known as the Red Planet?",
      correctOptionIndex: 2,
      correctAnswerText: "Mars",
      options: ["Venus", "Jupiter", "Mars", "Saturn"],
      mcqDistribution: { "0": 1, "1": 0, "2": 2, "3": 0 },
      timedOutCount: 0,
      playerAnswers: [
        { userId: "u1", userName: "Aragorn", selectedOption: 2, wasCorrect: true, pointsAwarded: 150 },
        { userId: "u2", userName: "Legolas", selectedOption: 2, wasCorrect: true, pointsAwarded: 100 },
        { userId: "u3", userName: "Gimli", selectedOption: 0, wasCorrect: false, pointsAwarded: 0 },
      ],
    },
    {
      round: 1,
      questionId: "q2",
      questionType: "TEXT_INPUT",
      questionText: "What is the capital of France?",
      correctOptionIndex: -1,
      correctAnswerText: "Paris",
      textSubmissions: [
        { text: "Paris", count: 2, isCorrect: true },
        { text: "paris", count: 1, isCorrect: true },
      ],
      timedOutCount: 0,
      playerAnswers: [
        { userId: "u1", userName: "Aragorn", selectedOption: -1, textAnswer: "Paris", wasCorrect: true, pointsAwarded: 150 },
        { userId: "u2", userName: "Legolas", selectedOption: -1, textAnswer: "paris", wasCorrect: true, pointsAwarded: 100 },
        { userId: "u3", userName: "Gimli", selectedOption: -1, textAnswer: "Lyon", wasCorrect: false, pointsAwarded: 0 },
      ],
    },
  ],
};
