"""Tiny independent mathematical checks for the Levitin puzzle lessons.

This file is NOT the teaching material. Read the proofs first; use code only
to verify small examples, invariants and optimal answers.
Python 3.11+, standard library only.
"""
from collections import deque
from math import isqrt


def magic_square_is_valid(rows):
    values = [v for row in rows for v in row]
    n = len(rows)
    if not n or any(len(row) != n for row in rows):
        return False
    if sorted(values) != list(range(1, n*n+1)):
        return False
    target = n*(n*n+1)//2
    return (
        all(sum(row) == target for row in rows)
        and all(sum(rows[i][j] for i in range(n)) == target for j in range(n))
        and sum(rows[i][i] for i in range(n)) == target
        and sum(rows[i][n-1-i] for i in range(n)) == target
    )


def queens_safe(rows):
    return (
        len(set(rows)) == len(rows)
        and all(abs(rows[i]-rows[j]) != abs(i-j)
                for i in range(len(rows))
                for j in range(i+1,len(rows)))
    )


def bridge_crossing_times():
    """The two canonical four-person schedules for times 1,2,5,10."""
    a,b,c,d = 1,2,5,10
    escort = a + 3*b + d
    solo = 2*a + b + c + d
    return escort, solo


def one_pair_glove_guarantee(pairs_by_colour):
    """Each colour may delay a match using all gloves of one hand."""
    return sum(pairs_by_colour)+1


def all_colours_glove_guarantee(pairs_by_colour):
    """All other colours paired; smallest colour postponed until last."""
    return 2*sum(pairs_by_colour)-min(pairs_by_colour)+1


def rectangle_triangle_counts(maximum):
    """Each right triangle can be replaced by two right triangles."""
    return list(range(2, maximum+1))


def min_ternary_experiments(hypotheses):
    k = 0
    while 3**k < hypotheses:
        k += 1
    return k


def page_number_digits(last_page):
    return sum(len(str(i)) for i in range(1,last_page+1))


def max_triangle_path(rows):
    best = rows[-1][:]
    for row in reversed(rows[:-1]):
        best = [v+max(best[j],best[j+1]) for j,v in enumerate(row)]
    return best[0]


def matches_for_champion(players):
    return players-1


def jug_shortest_path():
    capacity = (8,5,3)
    start = (8,0,0)
    q=deque([start])
    parents={start:None}
    while q:
        x=q.popleft()
        if 4 in x:
            path=[]
            while x is not None:
                path.append(x)
                x=parents[x]
            return list(reversed(path))
        for source in range(3):
            for dest in range(3):
                if source == dest:
                    continue
                amount = min(x[source],capacity[dest]-x[dest])
                if amount <= 0:
                    continue
                next_state=list(x)
                next_state[source]-=amount
                next_state[dest]+=amount
                next_state=tuple(next_state)
                if next_state not in parents:
                    parents[next_state]=x
                    q.append(next_state)
    return []


def rabbit_pairs(months):
    older, newer = 1,1
    if months in (0,1):
        return 1
    for _ in range(2,months+1):
        older, newer = newer, older+newer
    return newer


def open_lockers(n):
    return [i*i for i in range(1,isqrt(n)+1)]


def celebrity(matrix):
    n=len(matrix)
    if n == 0:
        return None
    candidate=0
    for i in range(1,n):
        if matrix[candidate][i]:
            candidate=i
    if all((not matrix[candidate][j] and matrix[j][candidate])
           for j in range(n) if j != candidate):
        return candidate
    return None


def sorted_table_lookup(rows,target):
    """Returns (found, cell count), given both row and column ascending order."""
    if not rows:
        return False,0
    i,j=0,len(rows[0])-1
    seen=0
    while i < len(rows) and j>=0:
        seen += 1
        value=rows[i][j]
        if value == target:
            return True,seen
        if value > target:
            j -= 1
        else:
            i += 1
    return False,seen


def floor_coverage(drops,eggs):
    """F(d,e) = F(d-1,e-1)+F(d-1,e)+1."""
    covered=[0]*(eggs+1)
    for _ in range(drops):
        for e in range(eggs,0,-1):
            covered[e]=covered[e]+covered[e-1]+1
    return covered[eggs]


def min_egg_drops(floors,eggs=2):
    drops=0
    while floor_coverage(drops,eggs) < floors:
        drops+=1
    return drops


def minimum_bits(cases):
    bits=0
    while 2**bits < cases:
        bits+=1
    return bits


def josephus_survivor(n):
    if n < 1:
        raise ValueError("At least one participant required")
    greatest_power=1 << (n.bit_length()-1)
    return 2*(n-greatest_power)+1


def river_distance():
    """Breadth-first search on safe four-bit bank states."""
    def safe(s):
        m,w,g,c=s
        return not ((w==g and m!=w) or (g==c and m!=g))
    initial=(0,0,0,0)
    goal=(1,1,1,1)
    q=deque([(initial,0)])
    visited={initial}
    while q:
        s,steps=q.popleft()
        if s==goal:
            return steps
        m=s[0]
        for passenger in (None,1,2,3):
            if passenger is not None and s[passenger]!=m:
                continue
            next_state=list(s)
            next_state[0]=1-m
            if passenger is not None:
                next_state[passenger]=1-next_state[passenger]
            next_state=tuple(next_state)
            if safe(next_state) and next_state not in visited:
                visited.add(next_state)
                q.append((next_state,steps+1))
    raise ValueError("No path")


if __name__ == "__main__":
    print("magic:",magic_square_is_valid([[8,1,6],[3,5,7],[4,9,2]]))
    print("queens:",queens_safe([2,4,1,3]))
    print("bridge alternatives:",bridge_crossing_times())
    print("river crossings:",river_distance())
    print("gloves one/all:",one_pair_glove_guarantee([5,3,2]),
          all_colours_glove_guarantee([5,3,2]))
    print("triangle max:",max_triangle_path([[2],[5,4],[3,4,7],[1,6,9,6]]))
    print("page count:",page_number_digits(562))
    print("jug path:",jug_shortest_path())
    print("rabbits month 12:",rabbit_pairs(12))
    print("locker count:",len(open_lockers(100)))
    print("2 eggs 100 floors:",min_egg_drops(100))
    print("1000 labels bits:",minimum_bits(1000))
    print("Josephus 41:",josephus_survivor(41))
