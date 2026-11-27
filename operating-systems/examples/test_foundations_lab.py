import unittest
from foundations_lab import (
    mean_wait_nonpreemptive, translate_address, evaluate_expression,
    typecheck_add, hadamard, probability, bell_probabilities
)


class CoreConcepts(unittest.TestCase):
    def test_shortest_first_wait(self):
        self.assertAlmostEqual(mean_wait_nonpreemptive([2, 4, 6]), 8/3)
        self.assertAlmostEqual(mean_wait_nonpreemptive([6, 4, 2]), 16/3)

    def test_paging_preserves_offset(self):
        self.assertEqual(translate_address(8197, 4096, {2: 7}), 28677)
        self.assertNotEqual(
            translate_address(8197, 4096, {2: 7}),
            translate_address(8197, 4096, {2: 12}),
        )
        with self.assertRaises(KeyError):
            translate_address(8197, 4096, {})

    def test_arithmetic_tree(self):
        self.assertEqual(evaluate_expression(2, "+", 3, "*", 4), 14)

    def test_integer_typing_rule(self):
        self.assertTrue(typecheck_add("Int", "Int"))
        self.assertFalse(typecheck_add("Int", "Bool"))

    def test_hadamard_probabilities(self):
        self.assertEqual(len(hadamard([1, 0])), 2)
        self.assertAlmostEqual(probability(hadamard([1, 0]))[0], .5)
        self.assertAlmostEqual(probability(hadamard([1, 0]))[1], .5)

    def test_two_hadamards_cancel(self):
        state = hadamard(hadamard([1, 0]))
        self.assertAlmostEqual(state[0], 1)
        self.assertAlmostEqual(state[1], 0)

    def test_bell_state(self):
        p = bell_probabilities()
        self.assertEqual(len(p), 4)
        for actual, expected in zip(p, [.5, 0, 0, .5]):
            self.assertAlmostEqual(actual, expected)


if __name__ == "__main__":
    unittest.main()
