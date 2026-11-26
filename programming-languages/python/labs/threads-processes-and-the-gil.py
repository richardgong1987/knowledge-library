# Run from programming-languages/python/labs with: python3 -I threads-processes-and-the-gil.py
# Timing checks use generous bounds; everything else is guaranteed by synchronisation.
import multiprocessing
import queue
import sys
import sysconfig
import threading
import time
from concurrent import interpreters
from concurrent.futures import InterpreterPoolExecutor, ProcessPoolExecutor, ThreadPoolExecutor


# Level 1: threads overlap waiting.
def fetch(page, results):
    time.sleep(0.05)  # stands in for a network call; sleeping releases the GIL
    results[page] = f"page {page}"


def fetch_all(pages):
    results = {}
    threads = [threading.Thread(target=fetch, args=(page, results)) for page in pages]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return results


# Level 2: which interpreter is this?
def gil_status():
    free_threaded_build = bool(sysconfig.get_config_var("Py_GIL_DISABLED"))
    return free_threaded_build, sys._is_gil_enabled()


# Level 3: "counter += 1" is a read followed by a write, and threads can interleave.
counter = 0


def racy_increment(barrier):
    global counter
    value = counter  # 1. read
    barrier.wait()  # both threads have now read the same value
    counter = value + 1  # 2. write: one of the two updates is lost


def lost_update():
    global counter
    counter = 0
    barrier = threading.Barrier(2)
    threads = [threading.Thread(target=racy_increment, args=(barrier,)) for _ in range(2)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return counter


lock = threading.Lock()


def safe_add(times):
    global counter
    for _ in range(times):
        with lock:  # read and write happen with no other thread in between
            counter += 1


def locked_total(threads_count, times):
    global counter
    counter = 0
    threads = [threading.Thread(target=safe_add, args=(times,)) for _ in range(threads_count)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return counter


# Level 4: a queue hands work from producers to consumers safely.
def producer(jobs, numbers):
    for n in numbers:
        jobs.put(n)  # blocks while the queue is full
    jobs.put(None)  # sentinel: no more work


def consumer(jobs, totals):
    total = 0
    while (n := jobs.get()) is not None:
        total += n
    totals.append(total)


def queue_total(numbers):
    jobs = queue.Queue(maxsize=2)
    totals = []
    workers = [threading.Thread(target=producer, args=(jobs, numbers)),
               threading.Thread(target=consumer, args=(jobs, totals))]
    for worker in workers:
        worker.start()
    for worker in workers:
        worker.join()
    return totals[0]


# Level 5: pools of threads, processes and interpreters.
def count_primes(limit):  # uses no module globals, so another interpreter can run it
    return sum(1 for n in range(2, limit) if all(n % d for d in range(2, int(n**0.5) + 1)))


visits = 0


def visit(n):
    global visits
    visits += n  # changes the child process's copy only
    return visits


def prime_worker(results, limit):
    results.put(count_primes(limit))


def primes_in_process(limit):
    results = multiprocessing.Queue()
    process = multiprocessing.Process(target=prime_worker, args=(results, limit))
    process.start()
    answer = results.get()
    process.join()
    return answer, process.exitcode


def primes_in_interpreter(limit):
    interp = interpreters.create()
    try:
        return interp.call(count_primes, limit)
    finally:
        interp.close()


# Level 6: how much faster can more workers make a program?
def amdahl(parallel_fraction, workers):
    return 1 / ((1 - parallel_fraction) + parallel_fraction / workers)


def check_threads():
    start = time.perf_counter()
    results = fetch_all(range(8))
    elapsed = time.perf_counter() - start
    assert results == {page: f"page {page}" for page in range(8)}
    assert elapsed < 0.3  # one after another would take 8 x 50 ms = 400 ms

    free_threaded_build, gil_enabled = gil_status()
    assert isinstance(gil_enabled, bool)
    if not free_threaded_build:
        assert gil_enabled  # the default build always has the GIL
    assert sys.getswitchinterval() == 0.005  # 5 ms

    assert lost_update() == 1  # two increments, one lost
    assert locked_total(4, 100_000) == 400_000
    assert queue_total(range(1, 11)) == 55


def check_pools():
    with ThreadPoolExecutor(max_workers=3) as pool:
        assert list(pool.map(len, ["a", "bb", "ccc"])) == [1, 2, 3]  # input order kept

    assert count_primes(10_000) == 1229
    with ProcessPoolExecutor(max_workers=2) as pool:
        assert list(pool.map(count_primes, [10_000] * 4)) == [1229] * 4
        assert pool.submit(visit, 5).result() == 5
    assert visits == 0  # the parent's variable never changed

    assert primes_in_process(10_000) == (1229, 0)
    assert primes_in_interpreter(10_000) == 1229
    with InterpreterPoolExecutor(max_workers=2) as pool:
        assert list(pool.map(count_primes, [1_000, 10_000])) == [168, 1229]

    assert round(amdahl(0.9, 4), 3) == 3.077
    assert round(amdahl(0.9, 1_000_000), 2) == 10.0
    assert round(amdahl(0.9, 8), 2) == 4.71


def main():
    check_threads()
    check_pools()
    print("OK")


if __name__ == "__main__":
    main()
