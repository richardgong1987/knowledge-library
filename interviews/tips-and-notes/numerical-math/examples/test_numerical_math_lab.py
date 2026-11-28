import unittest
from math import comb
from numerical_math_lab import (
 difference_of_squares,square_near,cube_difference,difference_of_powers,
 arithmetic_sum,arithmetic_progression,geometric_sum,square_sum,
 telescoping_reciprocal,inclusive_range_sum,modular_power,gcd_lcm_identity,
 ceil_nonnegative,sum_digit_count,xor_unique,count_set_bits,square_discriminant)

class NumericalAlgebraChecks(unittest.TestCase):
 def test_factorisations(self):
  self.assertEqual(difference_of_squares(13,7),(120,120))
  self.assertEqual(square_near(100,-2),(9604,9604))
  self.assertEqual(cube_difference(5,2),(117,117))
  self.assertEqual(difference_of_powers(3,2,4),(65,65))
  for a in range(-3,4):
   for b in range(-3,4):
    self.assertEqual(difference_of_squares(a,b)[0],difference_of_squares(a,b)[1])
    self.assertEqual(difference_of_powers(a,b,5)[0],difference_of_powers(a,b,5)[1])

 def test_sums(self):
  self.assertEqual(arithmetic_sum(5),15)
  self.assertEqual(arithmetic_sum(100),5050)
  self.assertEqual(arithmetic_progression(3,4,5),55)
  self.assertEqual(geometric_sum(2,4),15)
  self.assertEqual(square_sum(3),14)
  self.assertEqual(square_sum(10),385)
  self.assertAlmostEqual(telescoping_reciprocal(3),.75)
  self.assertEqual(inclusive_range_sum([2,4,1,5],1,3),10)

 def test_number_theory(self):
  self.assertEqual(gcd_lcm_identity(12,18),(216,216))
  self.assertEqual(modular_power(7,4,5),1)
  self.assertEqual(modular_power(3,8,7),pow(3,8,7))
  self.assertEqual(17%4,5%4)
  self.assertEqual(17-5,12)

 def test_bounds(self):
  self.assertEqual(ceil_nonnegative(17,5),4)
  self.assertEqual(square_discriminant(1,-5,6),1)
  self.assertEqual(square_discriminant(1,2,5),-16)
  self.assertLessEqual((4*9)**.5,(4+9)/2)
  self.assertGreaterEqual(7**2+3**2,2*7*3)

 def test_binary_and_counting(self):
  self.assertEqual(xor_unique([4,1,4,7,1]),7)
  self.assertEqual(count_set_bits(12),2)
  self.assertEqual(count_set_bits(40),2)
  self.assertEqual(comb(4,2),6)
  self.assertEqual(sum_digit_count(120),252)
  self.assertEqual(5<<2,20)
  self.assertEqual(5&4,4)

if __name__=="__main__":
 unittest.main()
