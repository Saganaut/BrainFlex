# BrainFlex TODO

## Frontend

### Design / Common Components

- [x] Button (`Common/Buttons/Btn.tsx`)
- [x] Card (`Common/Cards/Card.tsx`)
- [x] Modal / Dialog (`Common/Modal/index.tsx`)
- [x] Input / TextField
- [x] Textarea
- [ ] Select / Dropdown
- [x] Checkbox
- [x] Radio
- [ ] Toggle / Switch
- [x] Toast / Snackbar (`Common/Toast/Toast.tsx`)
- [ ] Tooltip
- [x] Badge / Tag / Chip
- [ ] Avatar
- [ ] Spinner / Loader
- [ ] Skeleton (loading placeholder)
- [ ] Progress Bar
- [ ] Tabs
- [ ] Pagination
- [ ] Empty State (zero-data UI)
- [ ] Alert / Banner (inline feedback)
- [ ] Divider
- [x] Icon Button (icon-only button variant)

### FRONTEND OTHER

- Navbar We shoudl have a preview and start (showcase) btn in the navbar (these are yet to be implemented)
- Left side bar
  - When creating a new deck we should have a first slide skeleton in the slide container
- Right side bar
  - should include a little vertical menu that includes the following icon btns:
  - Edit slide (pencil icon) When clicked it opens another drawer to the left of it that displays a bunch of slide edit options (yet to be decided)
  - Theme (palette icon) When clicked it opens another drawer to the left of it that displays a bunch of theme options (yet to be decided)
  - Partitcipant settings (users icon) When clicked it opens another drawer to the left of it that displays a list of participants and some options for each participant (yet to be decided)
  - Sharing preferences (share icon) When clicked it opens another drawer to the left of it that displays sharing options (yet to be decided)
- Below main slide area, we should have a drawer that can pop up where the user can add speaker notes, we should use our rich text editor for this.We need to add speaker notes to our data model also.
- ***

### Design / Game Components

- [ ] Live results display (real-time score updates)
- [ ] Player status indicators (e.g., active, waiting, disconnected)
- [ ] Question display (question text, options, timer)
- [ ] Leaderboard (ranking players based on scores)
- [ ] Game lobby (waiting room before game starts)
- [ ] Game over screen (final scores, winner announcement)
- [ ] Chat / Communication panel (for player interaction)
- [ ] Feedback / Response indicators (correct/incorrect answers)
- [ ] Control panel (for game host to manage game flow)
- [ ] Connected player info
- [ ] Connection status
- [ ] Game/quiz title
- [ ] Timer display (countdown for answering questions)
- [ ] Score display (current score for each player)
- [ ] Progress indicator (e.g., question number out of total)
- [ ] Join game screen (input for game code, player name)

## Backend

_(nothing tracked yet)_

TODO: Need proper frontend validation on all images uploads functionality + error messages otherwise silently fails with the following error in the backend:
2026-05-17T11:21:50.364Z WARN 43970 --- [brainflex] [nio-8080-exec-3] .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.web.multipart.MaxUploadSizeExceededException: Maximum upload size exceeded]
