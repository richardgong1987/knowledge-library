import unittest
from math import isclose
from linear_algebra_lab import (transform,add,dot,matrix_vector,determinant2,
    eliminate_example,projection,gaussian_fit_constant,transition,
    matrix_product,square,norm_squared,rotate90,gram_schmidt_second)

class MathematicsProofChecks(unittest.TestCase):
    def test_linear_map_and_basis(self):
        self.assertEqual(transform(3,2),(8,1))
        self.assertEqual(matrix_vector(((2,1),(1,-1)),(3,2)),(8,1))
        assert transform(3,2)==add(tuple(3*x for x in transform(1,0)),
                                  tuple(2*x for x in transform(0,1)))

    def test_vectors(self):
        self.assertEqual(add((3,2),(-1,4)),(2,6))
        self.assertEqual(dot((3,1),(1,-3)),0)
        self.assertEqual(projection((3,4),(1,0)),(3.0,0.0))
        self.assertEqual(gram_schmidt_second((1,0),(1,1)),(0.,1.))

    def test_matrix_computations(self):
        self.assertEqual(eliminate_example(),(1,6))
        self.assertEqual(determinant2(((2,1),(1,-1))),-3)
        self.assertEqual(determinant2(((1,2),(2,4))),0)
        A=((2,0),(0,1));B=((0,1),(1,0))
        self.assertEqual(matrix_product(B,A),((0,1),(2,0)))
        self.assertNotEqual(matrix_product(A,B),matrix_product(B,A))

    def test_geometry_and_least_squares(self):
        self.assertEqual(rotate90((3,2)),(-2,3))
        self.assertEqual(gaussian_fit_constant([1,2,3]),2)
        self.assertEqual(norm_squared((3,2)),norm_squared((-2,3)))

    def test_graph_two_step_walks(self):
        A=((0,1,1),(0,0,1),(0,0,0))
        self.assertEqual(square(A)[0][2],1)

    def test_markov(self):
        result=transition(((.9,.5),(.1,.5)),(.8,.2))
        self.assertTrue(isclose(result[0],.82))
        self.assertTrue(isclose(sum(result),1))

    def test_quantum_amplitude_norm(self):
        self.assertTrue(isclose(norm_squared((.6,.8)),1))
        X=((0,1),(1,0))
        self.assertEqual(matrix_vector(X,(1,0)),(0,1))
        self.assertEqual(matrix_vector(X,matrix_vector(X,(1,0))),(1,0))

if __name__=="__main__":
    unittest.main()
