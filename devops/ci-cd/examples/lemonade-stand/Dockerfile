FROM node:26.13.1-alpine3.24
WORKDIR /app
COPY package.json server.mjs ./
USER node
EXPOSE 8080
CMD ["node", "server.mjs"]
