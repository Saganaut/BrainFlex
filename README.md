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

### MongoDB

- Shell access:
  docker exec -it brainflex-mongodb-1 mongosh -u admin -p password

- Useful commands:
  - Show databases: `show dbs`
  - Use a database: `use brainflex`
  - Show collections: `show collections`
  - Find documents: `db.collection.find()`
  - Insert a document: `db.collection.insertOne({ key: 'value' })`
