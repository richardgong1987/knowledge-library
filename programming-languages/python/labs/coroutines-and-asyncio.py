# Run from programming-languages/python/labs with: python3 -I coroutines-and-asyncio.py
# The -I matters here: without it, this file's own name would shadow the asyncio module.
# Timing checks use generous bounds; orderings are only checked where asyncio guarantees them.
import asyncio
import contextlib
import time


# Level 1: calling an async function runs nothing; it creates a coroutine object.
async def greet(name):
    return f"hi {name}"


def drive_by_hand(coroutine):
    try:
        coroutine.send(None)  # run until the first suspension, or to the end
    except StopIteration as finished:
        return finished.value
    raise AssertionError("the coroutine suspended")


# Levels 2 and 3: await one at a time, or run tasks concurrently.
async def fetch(name, delay, log):
    log.append(f"start {name}")
    await asyncio.sleep(delay)  # suspends this coroutine; the loop runs others
    log.append(f"end {name}")
    return name


async def one_by_one(log):
    a = await fetch("a", 0.03, log)
    b = await fetch("b", 0.05, log)
    c = await fetch("c", 0.02, log)
    return [a, b, c]


async def all_at_once(log):
    return await asyncio.gather(fetch("a", 0.03, log), fetch("b", 0.05, log), fetch("c", 0.02, log))


# Level 4: a TaskGroup cancels the other tasks when one fails; gather does not.
async def fail_soon():
    await asyncio.sleep(0.01)
    raise ValueError("payment service down")


async def slow_job(log):
    try:
        await asyncio.sleep(1)
        log.append("finished")
    except asyncio.CancelledError:
        log.append("cancelled")
        raise  # never swallow cancellation


async def with_task_group(log):
    messages = []
    try:
        async with asyncio.TaskGroup() as group:
            group.create_task(slow_job(log))
            group.create_task(fail_soon())
    except* ValueError as errors:
        messages = [str(error) for error in errors.exceptions]  # "return" is not allowed here
    return messages


async def with_gather(log):
    job = asyncio.create_task(slow_job(log))
    try:
        await asyncio.gather(job, fail_soon())
    except ValueError:
        pass
    still_running = not job.done()  # gather left the other task running
    job.cancel()
    with contextlib.suppress(asyncio.CancelledError):
        await job
    return still_running


# Level 5: timeouts are cancellation with a deadline.
async def with_timeout():
    try:
        async with asyncio.timeout(0.02):
            await asyncio.sleep(1)
    except TimeoutError:
        return "timed out"


# Level 6: a blocking call freezes every task on the loop.
async def ticker(ticks):
    while True:
        await asyncio.sleep(0.005)
        ticks.append(time.perf_counter())


async def ticks_while(wait):
    ticks = []
    ticking = asyncio.create_task(ticker(ticks))
    await asyncio.sleep(0)  # let the ticker start
    before = len(ticks)
    await wait()
    during = len(ticks) - before
    ticking.cancel()
    return during


async def blocking_sleep():
    time.sleep(0.05)  # blocks the thread, and with it the whole loop


async def sleep_in_thread():
    await asyncio.to_thread(time.sleep, 0.05)  # the loop keeps running meanwhile


# Level 7: async iteration, async with, and a limit on concurrency.
async def countdown(n):
    while n > 0:
        await asyncio.sleep(0)  # an async generator may await between items
        yield n
        n -= 1


@contextlib.asynccontextmanager
async def connection(log):
    log.append("connect")
    try:
        yield "conn"
    finally:
        await asyncio.sleep(0)  # closing may have to wait too
        log.append("close")


async def limited(semaphore, state):
    async with semaphore:
        state["now"] += 1
        state["peak"] = max(state["peak"], state["now"])
        await asyncio.sleep(0.01)
        state["now"] -= 1


async def peak_concurrency(jobs, limit):
    semaphore = asyncio.Semaphore(limit)
    state = {"now": 0, "peak": 0}
    async with asyncio.TaskGroup() as group:
        for _ in range(jobs):
            group.create_task(limited(semaphore, state))
    return state["peak"]


async def timed(coroutine):
    start = time.perf_counter()
    result = await coroutine
    return result, time.perf_counter() - start


async def check_concurrency():
    log = []
    result, elapsed = await timed(one_by_one(log))
    assert result == ["a", "b", "c"] and elapsed >= 0.09  # 30 + 50 + 20 = 100 ms
    assert log == ["start a", "end a", "start b", "end b", "start c", "end c"]

    log = []
    result, elapsed = await timed(all_at_once(log))
    assert result == ["a", "b", "c"]  # gather keeps argument order
    assert log[:3] == ["start a", "start b", "start c"]  # all start before any ends
    assert elapsed < 0.09  # about max(30, 50, 20) = 50 ms

    log = []
    assert await with_task_group(log) == ["payment service down"]
    assert log == ["cancelled"]
    log = []
    assert await with_gather(log) is True and log == ["cancelled"]
    assert await with_timeout() == "timed out"


async def check_blocking_and_helpers():
    assert await ticks_while(blocking_sleep) == 0
    assert await ticks_while(sleep_in_thread) >= 1

    assert [n async for n in countdown(3)] == [3, 2, 1]
    log = []
    async with connection(log) as conn:
        log.append(f"use {conn}")
    assert log == ["connect", "use conn", "close"]

    peak, elapsed = await timed(peak_concurrency(6, 2))
    assert peak == 2  # never more than 2 inside the semaphore
    assert elapsed >= 0.025  # ceil(6 / 2) = 3 rounds of 10 ms


def main():
    coroutine = greet("Ada")
    assert type(coroutine).__name__ == "coroutine"
    assert drive_by_hand(coroutine) == "hi Ada"
    assert asyncio.run(greet("Ada")) == "hi Ada"
    asyncio.run(check_concurrency())
    asyncio.run(check_blocking_and_helpers())
    print("OK")


if __name__ == "__main__":
    main()
