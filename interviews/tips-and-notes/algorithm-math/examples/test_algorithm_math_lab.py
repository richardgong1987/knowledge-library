import unittest
from math import gcd,comb
from algorithm_math_lab import (
    prefix_sums,range_sum,count_target_subarrays,fixed_windows,
    add_interval,min_days,climb,knapsack_01,lis_length,
    count_inclusion_exclusion,number_of_subarrays,dijkstra_small_graph,
    kadane_nonempty,polynomial_hash,interval_optimum,bfs_shortest_path
)


class MathematicalPatterns(unittest.TestCase):
    def test_prefix_and_subarrays(self):
        self.assertEqual(prefix_sums([2,4,1,5]),[0,2,6,7,12])
        self.assertEqual(range_sum([2,4,1,5],1,3),10)
        self.assertEqual(count_target_subarrays([1,2,1],3),2)
        self.assertEqual(count_target_subarrays([1,-1,0],0),3)

    def test_sliding_and_difference(self):
        self.assertEqual(fixed_windows([2,4,1,5],3),[7,10])
        self.assertEqual(add_interval(5,1,3,3),[0,3,3,3,0])

    def test_monotonicity(self):
        self.assertEqual(min_days(17,5),4)
        self.assertEqual(bfs_shortest_path(),2)

    def test_dp_invariants(self):
        self.assertEqual(climb(0),1)
        self.assertEqual(climb(3),3)
        self.assertEqual(knapsack_01(3,[(2,3),(3,4)]),4)
        self.assertEqual(lis_length([3,5,2,4]),2)
        self.assertEqual(kadane_nonempty([-2,3,-1,4]),6)
        self.assertEqual(kadane_nonempty([-5,-2,-3]),-2)

    def test_greedy_exchange(self):
        self.assertEqual(interval_optimum([(1,2),(1,4),(2,3)]),2)

    def test_counting_and_number_theory(self):
        self.assertEqual(comb(4,2),6)
        self.assertEqual(count_inclusion_exclusion(8,6,3),11)
        self.assertEqual(number_of_subarrays(4),10)
        self.assertEqual(gcd(48,18),6)
        self.assertEqual(17%4,5%4)
        self.assertEqual(13*(13+1)//2,91)

    def test_advanced_examples(self):
        self.assertEqual(dijkstra_small_graph(),7)
        self.assertEqual(polynomial_hash([1,2]),7)
        self.assertEqual(polynomial_hash([2,1]),5)
        self.assertEqual(5&4,4)
        self.assertEqual(5&2,0)


if __name__=="__main__":
    unittest.main()
