"""Tiny dependency-free calculation lab for Computing Foundations.

This is an arithmetic check, NOT an OS scheduler, real compiler, or quantum
hardware simulator. Complex amplitudes are modelled exactly enough for the
deliberately simple one/two-qubit examples.
"""
from math import sqrt


def mean_wait_nonpreemptive(bursts):
    elapsed = 0
    waits = []
    for burst in bursts:
        if burst <= 0:
            raise ValueError("Burst must be positive")
        waits.append(elapsed)
        elapsed += burst
    return sum(waits) / len(waits)


def translate_address(virtual_address, page_size, page_to_frame):
    if page_size <= 0 or virtual_address < 0:
        raise ValueError("Invalid address or page size")
    page, offset = divmod(virtual_address, page_size)
    if page not in page_to_frame:
        raise KeyError("Unmapped page")
    return page_to_frame[page] * page_size + offset


def evaluate_expression(left, op, middle, times, right):
    """One miniature AST: Add(left, Mul(middle, right))."""
    if op != "+" or times != "*":
        raise ValueError("Only the +, * example is supported")
    return left + (middle * right)


def typecheck_add(type_a, type_b):
    return type_a == "Int" and type_b == "Int"


def hadamard(state):
    if len(state) != 2:
        raise ValueError("Expected one qubit with two amplitudes")
    a, b = state
    return [(a + b) / sqrt(2), (a - b) / sqrt(2)]


def probability(state):
    return [abs(a) ** 2 for a in state]


def bell_probabilities():
    """Apply H on first qubit and CNOT on the result |00>."""
    state = [1 + 0j, 0j, 0j, 0j]  # |00>, |01>, |10>, |11>
    h_first = [
        (state[0] + state[2]) / sqrt(2),
        (state[1] + state[3]) / sqrt(2),
        (state[0] - state[2]) / sqrt(2),
        (state[1] - state[3]) / sqrt(2),
    ]
    h_first[2], h_first[3] = h_first[3], h_first[2]  # CNOT swap
    return probability(h_first)


if __name__ == "__main__":
    print("SJF average wait", mean_wait_nonpreemptive([2, 4, 6]))
    print("Virtual 8197 -> physical", translate_address(8197, 4096, {2: 7}))
    print("Parsed expression:", evaluate_expression(2, "+", 3, "*", 4))
    print("Hadamard probabilities:", probability(hadamard([1, 0])))
    print("Bell probabilities:", bell_probabilities())
