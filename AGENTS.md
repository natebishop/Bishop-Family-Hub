# Bishop Family Hub

This is a monorepo containing the FamilyHub frontend and backend.

## Repository layout

- `frontend/` — Vite/React/PWA application. Run frontend commands from this directory.
- `backend/` — Spring Boot API. Run backend commands from this directory.
- `PROJECT_SETUP.md` — deployment and repository migration notes.

## Important deployment context

The current production deployments still build from the original standalone GitHub repositories:

- `natebishop/FamilyHub` for Vercel
- `natebishop/family-hub-api` for DigitalOcean

Do not change Vercel or DigitalOcean source settings unless the user explicitly asks to complete the monorepo deployment migration.

Never commit `.env.local`, database credentials, OAuth secrets, JWT secrets, or other local credentials.
