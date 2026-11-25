# Issue tracker: Local Markdown (`/Users/ramitsuri/Documents/SideProjects`)

Issues and specs for this repo live as markdown files in `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/`. Create `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/` if it does not already exist.

## Conventions

- One feature per directory: `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<feature-slug>/`
- The spec is `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<feature-slug>/spec.md`
- Implementation issues are one file per ticket at `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01`, never a single combined tickets file
- Status is recorded as a `Status:` line near the top of each issue file (e.g. `open`, `in-progress`, `closed`)
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket.

- **Map**: `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<effort>/map.md` (the Notes / Decisions-so-far / Fog body).
- **Child ticket**: `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<effort>/issues/NN-<slug>.md`, numbered from `01`, with the question in the body. A `Type:` line records the ticket type (`research`/`prototype`/`grilling`/`task`); a `Status:` line records `claimed`/`resolved`.
- **Blocking**: a `Blocked by: NN, NN` line near the top. A ticket is unblocked when every file it lists is `resolved`.
- **Frontier**: scan `/Users/ramitsuri/Documents/SideProjects/NotificationJournal/<effort>/issues/` for files that are open, unblocked, and unclaimed; first by number wins.
- **Claim**: set `Status: claimed` and save before any work.
- **Resolve**: append the answer under an `## Answer` heading, set `Status: resolved`, then append a context pointer (gist + link) to the map's Decisions-so-far in `map.md`.
