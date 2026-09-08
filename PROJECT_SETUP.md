# Bishop Family Hub project setup

This is the local monorepo for the FamilyHub installation.

## Layout

- `frontend/` — Vite/React PWA. Current deployed source is the `natebishop/FamilyHub` GitHub fork.
- `backend/` — Spring Boot API. Current deployed source is the `natebishop/family-hub-api` GitHub fork.

## Current deployment safety

The existing Vercel and DigitalOcean deployments still use the two original standalone GitHub repositories. This monorepo is a local consolidation of their current source state; deployment settings have not been changed yet.

Do not delete or rewrite the standalone repositories until the monorepo has been tested and a deliberate migration of Vercel and DigitalOcean has been completed.

## Original remotes

Frontend:

- origin: `https://github.com/natebishop/FamilyHub.git`
- upstream: `https://github.com/joe-bor/FamilyHub.git`

Backend:

- origin: `https://github.com/natebishop/family-hub-api.git`
- upstream: `https://github.com/joe-bor/family-hub-api.git`

## Local development

Run frontend commands from `frontend/`.

Run backend commands from `backend/`.

The frontend's `.env.local` is intentionally not copied into this repository. Create it locally from `frontend/.env.example` when needed.

## Next migration step

After the monorepo builds successfully, create or select a GitHub repository named `Bishop-Family-Hub`, push this repository, then update Vercel to build from `frontend/` and DigitalOcean to build from `backend/`. Keep the current deployments unchanged until both migrations are verified.
