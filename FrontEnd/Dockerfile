FROM node:16-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY src ./src
COPY public ./public

# Set permissions for node_modules and create cache directory
RUN mkdir -p /app/node_modules/.cache && \
      chmod -R 777 /app/node_modules/.cache

# Disable ESLint to avoid permission issues in development
ENV DISABLE_ESLINT_PLUGIN=true

# Expose port
EXPOSE 3000

# Start development server
CMD ["npm", "start"]
