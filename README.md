### BrainFlex

## Based on Cephadex Games

## Goal is to create a paired down version of the a previous project while learning new technologies (MongoDb, Java, Spring Boot)

### BACKEND

- JAVA - Spring Boot
- MongoDB
- Redis

### FRONTEND

- React
- Typescript
- React Compiler
- Vite
- ESLint (ReactX, ReactDOM, CSS plugins)

### Garage (S3-compatible object storage)

First-time setup — run once after `docker compose up -d`:

```bash
# 1. Get your node ID (copy the long hex string at the start)
docker compose exec garage /garage node id

# 2. Assign layout (replace <NODE_ID> with the first 8+ chars from step 1)
docker compose exec garage /garage layout assign -z dc1 -c 1G <NODE_ID>
docker compose exec garage /garage layout apply --version 1

# 3. Create the access key (output will show Key ID and Secret key — copy both)
docker compose exec garage /garage key create brainflex-key

# 4. Create the bucket and grant access
docker compose exec garage /garage bucket create brainflex-images
docker compose exec garage /garage bucket allow brainflex-images --read --write --key brainflex-key

# 5. Add the keys to .env
# S3_ACCESS_KEY="<Key ID from step 3>"
# S3_SECRET_KEY="<Secret key from step 3>"
```

Or run the automated script (does steps 1–4 and prints the keys):
```bash
bash scripts/init-garage.sh
```

Useful commands:
- List buckets: `docker compose exec garage /garage bucket list`
- List keys: `docker compose exec garage /garage key list`
- Check cluster status: `docker compose exec garage /garage status`

### MongoDB

- Shell access:
  docker exec -it brainflex-mongodb-1 mongosh -u admin -p password

- Useful commands:
  - Show databases: `show dbs`
  - Use a database: `use brainflex`
  - Show collections: `show collections`
  - Find documents: `db.collection.find()`
  - Insert a document: `db.collection.insertOne({ key: 'value' })`
