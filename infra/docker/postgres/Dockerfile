# pg18 + postgis + pgvector. no official image ships both.
FROM postgis/postgis:18-3.6

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-18-pgvector \
    && rm -rf /var/lib/apt/lists/*

# baked in, not bind-mounted: a remote docker daemon resolves binds on its own host.
COPY init/ /docker-entrypoint-initdb.d/
