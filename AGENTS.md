# Bishop Family Hub

This is a monorepo containing the FamilyHub frontend and backend.

## Repository layout

- `frontend/` — Vite/React/PWA application. Run frontend commands from this directory.
- `backend/` — Spring Boot API. Run backend commands from this directory.
- `PROJECT_SETUP.md` — deployment and repository migration notes.

## Important deployment context

The production deployments use the monorepo GitHub repository:

- `natebishop/Bishop-Family-Hub`
- Vercel project `familyhub` builds from `frontend/`
- DigitalOcean App Platform app `familyhub-api` builds from `backend/`

The original standalone repositories are retained temporarily for rollback only. Do not delete or rewrite them without an explicit request.

Never commit `.env.local`, database credentials, OAuth secrets, JWT secrets, or other local credentials.
