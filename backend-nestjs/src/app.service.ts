import { Injectable, Inject } from '@nestjs/common';
import Redis from 'ioredis';

@Injectable()
export class AppService {
  // Wir nutzen @Inject, um unsere eigene 'Fabrik' anzuzapfen
  constructor(@Inject('REDIS_CLIENT') private readonly redis: Redis) {}

  async testConnection(): Promise<string> {
    try {
      const result = await this.redis.ping();
      return `Antwort vom Server-Redis: ${result}`;
    } catch (error) {
      return `Fehler: ${error.message}`;
    }
  }
}