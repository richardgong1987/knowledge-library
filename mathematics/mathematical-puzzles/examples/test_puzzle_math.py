import unittest
from puzzle_math import (
    magic_square_is_valid, queens_safe, bridge_crossing_times,
    one_pair_glove_guarantee, all_colours_glove_guarantee,
    rectangle_triangle_counts, min_ternary_experiments, page_number_digits,
    max_triangle_path, matches_for_champion, jug_shortest_path,
    rabbit_pairs, open_lockers, celebrity, sorted_table_lookup,
    floor_coverage, min_egg_drops, minimum_bits, josephus_survivor,
    river_distance,
)

class PuzzleProofChecks(unittest.TestCase):
    def test_magic_square(self):
        self.assertTrue(magic_square_is_valid([[8,1,6],[3,5,7],[4,9,2]]))
        self.assertFalse(magic_square_is_valid([[1,2,3],[4,5,6],[7,8,9]]))

    def test_four_queens(self):
        self.assertTrue(queens_safe([2,4,1,3]))
        self.assertFalse(queens_safe([1,3,2,4]))

    def test_bridge_optimum(self):
        self.assertEqual(bridge_crossing_times(),(17,19))

    def test_river_crossing(self):
        self.assertEqual(river_distance(),7)

    def test_glove_guarantees(self):
        self.assertEqual(one_pair_glove_guarantee([5,3,2]),11)
        self.assertEqual(all_colours_glove_guarantee([5,3,2]),19)

    def test_right_triangle_construction(self):
        self.assertEqual(rectangle_triangle_counts(6),[2,3,4,5,6])

    def test_eight_coins_information(self):
        self.assertEqual(min_ternary_experiments(8),2)
        self.assertEqual(min_ternary_experiments(27),3)

    def test_page_numbering(self):
        self.assertEqual(page_number_digits(562),1578)
        self.assertEqual(page_number_digits(120),252)

    def test_maximum_triangle_path(self):
        tri=[[2],[5,4],[3,4,7],[1,6,9,6]]
        self.assertEqual(max_triangle_path(tri),22)

    def test_tournament_count(self):
        self.assertEqual(matches_for_champion(8),7)
        self.assertEqual(matches_for_champion(10),9)

    def test_water_jugs(self):
        path=jug_shortest_path()
        self.assertEqual(len(path)-1,6)
        self.assertEqual(path[0],(8,0,0))
        self.assertEqual(path[-1],(1,4,3))
        self.assertTrue(all(sum(s)==8 for s in path))

    def test_rabbits(self):
        self.assertEqual([rabbit_pairs(i) for i in range(6)],[1,1,2,3,5,8])
        self.assertEqual(rabbit_pairs(12),233)

    def test_lockers(self):
        self.assertEqual(open_lockers(20),[1,4,9,16])
        self.assertEqual(len(open_lockers(100)),10)

    def test_celebrity(self):
        # Person 2 knows nobody; everyone else knows person 2.
        a=[[False,True,True],[True,False,True],[False,False,False]]
        # Here person 2 is the only celebrity.
        self.assertEqual(celebrity(a),2)
        self.assertIsNone(celebrity([[False,True],[True,False]]))

    def test_sorted_table(self):
        table=[[1,3,8],[2,5,9],[4,7,10]]
        self.assertEqual(sorted_table_lookup(table,7),(True,4))
        self.assertLessEqual(sorted_table_lookup(table,6)[1],5)
        big=[[i*10+j for j in range(10)] for i in range(10)]
        self.assertLessEqual(sorted_table_lookup(big,100)[1],19)

    def test_eggs(self):
        self.assertEqual(floor_coverage(13,2),91)
        self.assertEqual(floor_coverage(14,2),105)
        self.assertEqual(min_egg_drops(100),14)
        self.assertEqual(min_egg_drops(36),8)

    def test_bits(self):
        self.assertEqual(minimum_bits(1000),10)
        self.assertEqual(format(517,"010b"),"1000000101")
        self.assertEqual(minimum_bits(250),8)

    def test_josephus(self):
        self.assertEqual(josephus_survivor(1),1)
        self.assertEqual(josephus_survivor(10),5)
        self.assertEqual(josephus_survivor(41),19)
        with self.assertRaises(ValueError):
            josephus_survivor(0)

if __name__ == "__main__":
    unittest.main()
