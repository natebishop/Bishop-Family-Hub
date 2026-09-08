# Bishop Family Hub project setup

This is the local monorepo for the FamilyHub installation.

## Layout

- `frontend/` — Vite/React PWA. Vercel builds this directory.
- `backend/` — Spring Boot API. DigitalOcean App Platform builds this directory.

## Deployment source of truth

The production deployments now use the single GitHub repository:

- Repository: `natebishop/Bishop-Family-Hub`
- Vercel project: `familyhub`, root directory `frontend/`
- DigitalOcean App Platform app: `familyhub-api`, source directory `/backend`

Pushes to `main` are the normal deployment path for both services. The original standalone repositories are retained temporarily as rollback references; do not delete or rewrite them until the migrated deployments have had a reasonable period of successful operation.

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

## Deployment verification

After changing deployment configuration, verify the Vercel frontend URL and DigitalOcean API health endpoint before treating the migration as complete. Keep provider credentials and local environment files out of the repository.
