# Publish Database Seeder

## Build

```bash
docker build -t mecanica-dm-seeder -f Dockerfile.seeder .
```

## Run locally

```bash
docker run --rm --env-file .env mecanica-dm-seeder
```

## Publish to a registry

Replace `<registry>` with your registry URL (e.g., `docker.io`, `ghcr.io`).

```bash
docker tag mecanica-dm-seeder diegopriess/mecanica-dm-seeder:latest
docker push diegopriess/mecanica-dm-seeder:latest
```
