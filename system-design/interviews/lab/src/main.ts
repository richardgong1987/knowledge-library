import 'reflect-metadata';
import { Body, Controller, Get, Injectable, Module, Post, Query } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import {
  Autocomplete, chunkFile, DeficitScheduler, HybridFeed, IdempotencyGate,
  mergeTopK, PoliteFrontier, rendezvous, SeatHolds, SequencedInbox,
  TokenBucket, VersionedCache
} from './core';

/**
 * Single-process teaching API. No authentication, storage, or replica coordination.
 * Run localhost only. All policy logic lives in core.ts and is tested separately.
 */
@Injectable()
class LabState {
  readonly scheduler = new DeficitScheduler(2);
  readonly buckets = new TokenBucket(3, 1);
  readonly dedupe = new IdempotencyGate<{result:string,invocations:number}>();
  readonly cache = new VersionedCache<string>();
  readonly inbox = new SequencedInbox<string>();
  readonly seats = new SeatHolds();
  readonly feed = new HybridFeed(3);
  readonly search = new Autocomplete();
  readonly frontier = new PoliteFrontier();
  invocations = 0;
}

@Controller('lab')
class LabController {
  constructor(private readonly state: LabState) {}
  @Get('health')
  health() { return {status:'ok',example:'system-design-interview-lab'}; }

  @Post('jobs')
  enqueue(@Body() body: {tenant:string,id:string,cost:number,weight?:number}) {
    this.state.scheduler.enqueue({tenant:body.tenant,id:body.id,cost:body.cost},body.weight??1);
    return {queued:this.state.scheduler.depth()};
  }
  @Post('files')
  file(@Body() body: {tenant:string,fileId:string,units:number,chunkSize:number}) {
    const tasks=chunkFile(body.tenant,body.fileId,body.units,body.chunkSize);
    for(const job of tasks)this.state.scheduler.enqueue(job);
    return {chunks:tasks.length,queued:this.state.scheduler.depth()};
  }
  @Get('jobs/next')
  nextJob() { return this.state.scheduler.next() ?? {empty:true}; }

  @Post('rate')
  rate(@Body() body: {key:string,nowMs:number,cost?:number}) {
    return {allowed:this.state.buckets.allow(body.key,body.nowMs,body.cost??1)};
  }
  @Post('idempotency')
  async idempotent(@Body() body:{key:string,fingerprint:string}) {
    return this.state.dedupe.run(body.key,body.fingerprint,async()=>{
      this.state.invocations++;
      return {result:'created',invocations:this.state.invocations};
    });
  }
  @Post('cache')
  putCache(@Body() body:{key:string,version:number,value:string}) {
    return {accepted:this.state.cache.put(body.key,body.version,body.value)};
  }
  @Get('cache')
  cache(@Query('key') key:string) { return this.state.cache.get(key) ?? {missing:true}; }
  @Post('sequence')
  event(@Body() body:{key:string,seq:number,value:string}) {
    return this.state.inbox.receive(body.key,body.seq,body.value);
  }
  @Get('sequence')
  ordered(@Query('key') key:string) { return this.state.inbox.history(key); }
  @Get('shard')
  shard(@Query('key') key:string) {
    return {node:rendezvous(key,['A','B','C'])};
  }
  @Post('seat/hold')
  hold(@Body() body:{seat:string,owner:string,nowMs:number,ttlMs:number}) {
    return {token:this.state.seats.hold(body.seat,body.owner,body.nowMs,body.ttlMs)??null};
  }
  @Post('seat/confirm')
  confirm(@Body() body:{seat:string,owner:string,token:number,nowMs:number}) {
    return {confirmed:this.state.seats.confirm(body.seat,body.owner,body.token,body.nowMs)};
  }
  @Post('feed/follow')
  follow(@Body() body:{reader:string,author:string}) {
    this.state.feed.follow(body.reader,body.author);
    return {followed:true};
  }
  @Post('feed/post')
  post(@Body() body:{id:string,author:string,time:number}) {
    return {mode:this.state.feed.publish(body)};
  }
  @Get('feed')
  feed(@Query('reader') reader:string) { return this.state.feed.read(reader); }
  @Post('search/record')
  record(@Body() body:{term:string}) {
    this.state.search.record(body.term);
    return {ok:true};
  }
  @Get('search/suggest')
  suggest(@Query('prefix') prefix:string) { return this.state.search.suggest(prefix??''); }
  @Post('crawl')
  crawl(@Body() body:{url:string}) { return {added:this.state.frontier.enqueue(body.url)}; }
  @Get('crawl/next')
  nextUrl(@Query('nowMs') nowMs:string,@Query('delayMs') delayMs:string) {
    return {url:this.state.frontier.claim(Number(nowMs),Number(delayMs))??null};
  }
  @Post('rank/merge')
  topK(@Body() body:{shards:Array<Array<{id:string,score:number}>>,k:number}) {
    return mergeTopK(body.shards,body.k);
  }
}
@Module({controllers:[LabController],providers:[LabState]})
class AppModule {}
async function main() {
  const app=await NestFactory.create(AppModule);
  await app.listen(Number(process.env.PORT??4300),'127.0.0.1');
  console.log('Interview lab: http://127.0.0.1:'+(process.env.PORT??4300)+'/lab/health');
}
void main();
