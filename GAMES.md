/\*\*

- Create
- \*\* Game "Play"
- **\*** Auto generate
- **\*** Create your own
- **\*** Choose a template
-
-
- \*\* Audience Polling "Pulse"
- **\*** Auto generate
- **\*** Create your own
- **\*** Choose a template
- \*\* Join
- \*\*\*\* Enter code
-
-
-
- Types of game questions:
- - Multiple choice
- - Type in (players type in their answers, correct answers get points)
- ---- Can be text or number
- - Re-order (Put the following cities in order of oldest to newest..)
- -- All these have variants where after all players submit their answer, their answers are displayed anonymously and in the next step
- players vote on which answer they think is correct (like dixit)
- -- Always have options to have a timer
- -- Can decide how many points each question is worth (defaults to 1), and how many points the variant where we can guess who got the right answer.
- -- Option to show scores/results immediately or only after the end
- -- Option to reveal answers immediately (but no player should be able to answer after it is done obviously)
- -- Option so that players can join after the game has started
- -- Should allow players to reconnect if they get disconnected
- -- Should have indicators of players that are disconnected or idle
- -- Admin should be able to ban or silence players
- -- Indicator to show when players have answered
- -- Option to display how many players answered gave each response (for example a progress bar on a MCQ questions, a word cloud for the input questions)
-
- Audience polling interaction...
- - Same items as in games but we are not giving/counting points. Instead the focus is on data tracking.
- -- Open to suggestions here.
- \*\*/

- Template:we will have a few predefined templates with various questions and themes (e.g., general knowledge, movies, science, etc.) that users can choose from to quickly set up a game or poll.
- Custom creation: users can create their own games or polls from scratch, allowing them to input their own questions, answer options, and settings.
- Auto-generation: users can input a topic or theme, and the system will automatically generate a set of questions and answers based on that input, using AI to create engaging and relevant content. They will also be able to upload their own content, for example a PDF, a webpage, or a document, and the system will extract information from it to create questions and answers.

The main page of the app @MainPage will immediately prompt users to create a game or poll or join an existing one.

The flow should be as follows:
--> Create Game --> option to choose template, auto-generate, or create custom

--> Choose template --> list of templates --> select template --> customize (optional) --> start game

Since we will have a lot of customization options we don't want to overwhelm the users, so we should focus on teh quick start. They go from one screen to the next as quickly as possible. Customization options don't have to be shown, it can be something they discover as they go.

We should use existing components as much as possible, and create other components following our projects rules as needed. Make sure any new component shows up in our design-system page.
