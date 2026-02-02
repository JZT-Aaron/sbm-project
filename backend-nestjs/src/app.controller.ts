import { Controller, Get } from '@nestjs/common';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  // Wir löschen den alten 'getHello' Part und nutzen unseren Test
  @Get('redis-test')
  getRedisTest() {
    return this.appService.testConnection();
  }
}
