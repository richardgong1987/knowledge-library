"""Tests for the original, standard-library-only learning example."""
import importlib.util
import math
import pathlib
import unittest

PATH=pathlib.Path(__file__).with_name("delivery_ml_lab.py")
spec=importlib.util.spec_from_file_location("delivery_ml_lab",PATH)
lab=importlib.util.module_from_spec(spec)
spec.loader.exec_module(lab)

class LearningLabTests(unittest.TestCase):
    def test_linear_system(self):
        x=lab.solve([[2.,1.],[1.,3.]],[5.,7.])
        self.assertAlmostEqual(2*x[0]+x[1],5)
        self.assertAlmostEqual(x[0]+3*x[1],7)
    def test_regression_gradient(self):
        expected=lab.fit_ols()
        actual=lab.gradient_descent()
        self.assertLess(max(abs(a-b) for a,b in zip(expected,actual)),0.03)
        self.assertLess(lab.squared_error(actual),lab.squared_error([0,0,0]))
    def test_pca(self):
        mean,cov,eig,direction,scores=lab.principal_component()
        self.assertAlmostEqual(math.hypot(*direction),1.,places=8)
        self.assertAlmostEqual(sum(scores),0.,places=8)
        self.assertGreater(eig,0)
    def test_gmm(self):
        weights,means,variances=lab.gmm_em()
        self.assertAlmostEqual(sum(weights),1.,places=6)
        self.assertTrue(all(0<w<1 for w in weights))
        self.assertTrue(all(v>0 for v in variances))
    def test_svm(self):
        margins,margin=lab.svm_toy()
        self.assertGreaterEqual(min(margins),1.)
        self.assertAlmostEqual(margin,0.5)
if __name__=="__main__":
    unittest.main()
