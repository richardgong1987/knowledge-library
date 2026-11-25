import math
import unittest
from finance_lab import (
    present_value, bond_cashflows, bond_price, bond_price_spot,
    yield_to_maturity, macaulay_duration, modified_duration, convexity, dv01,
    forward_rate, bootstrap_two_year, dirty_price, portfolio_return,
    portfolio_variance, portfolio_duration, tracking_error,
    capm_required_return, credit_spread_approx, one_period_default_price,
    forward_delivery_price, call_option_one_step
)

class FinanceExamples(unittest.TestCase):
    def test_two_year_bond(self):
        cash = bond_cashflows()
        self.assertEqual(cash, [5., 105.])
        self.assertAlmostEqual(bond_price(.05), 100.)
        self.assertAlmostEqual(bond_price(.06), 98.16660733, places=6)
        self.assertAlmostEqual(yield_to_maturity(100., cash), .05, places=10)

    def test_interest_rate_risk(self):
        cash = [5., 105.]
        self.assertAlmostEqual(macaulay_duration(cash, .05), 1.952380952, places=8)
        self.assertAlmostEqual(modified_duration(cash, .05), 1.859410431, places=8)
        self.assertAlmostEqual(convexity(cash, .05), 5.269409351, places=8)
        self.assertAlmostEqual(dv01(cash, .05), 0.0185941043, places=8)
        self.assertGreater(bond_price(.04), bond_price(.05))

    def test_curve_and_forward(self):
        self.assertAlmostEqual(bond_price_spot([5.,105.], [.04,.05]),
                               100.0457875458, places=8)
        self.assertAlmostEqual(forward_rate(.04, .05), .060096153846, places=8)
        self.assertAlmostEqual(bootstrap_two_year(5, 100, .04),
                               .05025249489, places=8)

    def test_quotes(self):
        self.assertAlmostEqual(dirty_price(101.2, 1.5), 102.7)

    def test_portfolio(self):
        w = [.5, .5]
        self.assertAlmostEqual(portfolio_return(w, [.06,.10]), .08)
        self.assertAlmostEqual(math.sqrt(portfolio_variance(
            w, [[.01,0],[0,.04]])), .11180339887498)
        self.assertAlmostEqual(portfolio_duration([60,40],[2,7]),4.)
        self.assertAlmostEqual(tracking_error([.01,-.01]), .01)
        self.assertAlmostEqual(capm_required_return(.02,1.2,.08), .092)

    def test_credit(self):
        self.assertAlmostEqual(credit_spread_approx(.02,.4), .012)
        self.assertAlmostEqual(one_period_default_price(100,.02,.4,.03),
                               95.922330097, places=8)

    def test_derivatives(self):
        self.assertAlmostEqual(forward_delivery_price(100,.05,1),105.)
        self.assertAlmostEqual(call_option_one_step(120,80,100,.5,.05),
                               9.5238095238, places=8)

    def test_invalid_inputs(self):
        with self.assertRaises(ValueError):
            yield_to_maturity(1000,[5,105])
        with self.assertRaises(ValueError):
            bond_price_spot([5,105],[.05])
        with self.assertRaises(ValueError):
            portfolio_variance([.5,.5],[[1]])
        with self.assertRaises(ValueError):
            credit_spread_approx(.02,1.2)

if __name__ == "__main__":
    unittest.main()
