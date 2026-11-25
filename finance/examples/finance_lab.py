"""Tiny original examples for the Knowledge Library finance lessons.

All rates are DECIMALS: 5% is 0.05, and 1 basis point is 0.0001.
Examples are annual and educational, not a production bond-pricing engine.
No external dependencies.
"""
from __future__ import annotations
from math import sqrt
from typing import Sequence


def present_value(cashflows: Sequence[float], rate: float) -> float:
    """cashflows[0] arrives in one year; annual flat discount rate."""
    if rate <= -1:
        raise ValueError("Rate must be greater than -100%")
    return sum(cf / (1 + rate) ** (t + 1) for t, cf in enumerate(cashflows))


def bond_cashflows(face: float = 100, coupon_rate: float = 0.05,
                   years: int = 2) -> list[float]:
    """Annual fixed coupon; face repaid with last coupon."""
    if face <= 0 or years < 1 or int(years) != years:
        raise ValueError("Positive face and integer maturity required")
    coupons = [face * coupon_rate] * years
    coupons[-1] += face
    return coupons


def bond_price(yield_rate: float, face: float = 100, coupon_rate: float = 0.05,
               years: int = 2) -> float:
    return present_value(bond_cashflows(face, coupon_rate, years), yield_rate)


def bond_price_spot(cashflows: Sequence[float], spot_rates: Sequence[float]) -> float:
    """Each cash flow is discounted by its own annually compounded zero rate."""
    if len(cashflows) != len(spot_rates):
        raise ValueError("One spot rate per maturity is required")
    if any(r <= -1 for r in spot_rates):
        raise ValueError("Invalid spot rate")
    return sum(cf / (1 + r) ** (i + 1)
               for i, (cf, r) in enumerate(zip(cashflows, spot_rates)))


def yield_to_maturity(price: float, cashflows: Sequence[float],
                      low: float = -0.5, high: float = 1.0) -> float:
    """Bisection for positive cash flows when price lies in the bracket."""
    if price <= 0 or not cashflows or any(c < 0 for c in cashflows):
        raise ValueError("Positive price and nonnegative cashflows required")
    if low <= -1:
        raise ValueError("Lower yield must exceed -100%")
    if not (present_value(cashflows, high) <= price <= present_value(cashflows, low)):
        raise ValueError("Price not bracketed by yields")
    for _ in range(100):
        mid = (low + high) / 2
        if present_value(cashflows, mid) > price:
            low = mid
        else:
            high = mid
    return (low + high) / 2


def macaulay_duration(cashflows: Sequence[float], rate: float) -> float:
    """PV-weighted average cash-flow time, measured in years."""
    p = present_value(cashflows, rate)
    if p <= 0:
        raise ValueError("Positive bond price needed")
    return sum((i + 1) * cf / (1 + rate) ** (i + 1)
               for i, cf in enumerate(cashflows)) / p


def modified_duration(cashflows: Sequence[float], rate: float) -> float:
    return macaulay_duration(cashflows, rate) / (1 + rate)


def convexity(cashflows: Sequence[float], rate: float) -> float:
    """Annual-compounding analytical second derivative divided by price."""
    p = present_value(cashflows, rate)
    return sum((i + 1) * (i + 2) * cf / (1 + rate) ** (i + 3)
               for i, cf in enumerate(cashflows)) / p


def dv01(cashflows: Sequence[float], rate: float) -> float:
    """Approximate positive dollar loss from a one-basis-point yield rise."""
    return present_value(cashflows, rate) * modified_duration(cashflows, rate) * 0.0001


def forward_rate(spot_1: float, spot_2: float) -> float:
    """One-year forward rate from end of year one to end of year two."""
    return (1 + spot_2) ** 2 / (1 + spot_1) - 1


def bootstrap_two_year(coupon: float, bond_price_now: float, spot_1: float,
                       face: float = 100) -> float:
    """Solve year-two zero rate from price, year-one spot and two-year coupons."""
    discount_1 = 1 / (1 + spot_1)
    discount_2 = (bond_price_now - coupon * discount_1) / (face + coupon)
    if discount_2 <= 0:
        raise ValueError("Inconsistent price/discount factor")
    return discount_2 ** (-0.5) - 1


def dirty_price(clean_price: float, accrued_interest: float) -> float:
    return clean_price + accrued_interest


def portfolio_return(weights: Sequence[float], expected: Sequence[float]) -> float:
    if len(weights) != len(expected):
        raise ValueError("Matching lengths required")
    return sum(w * r for w, r in zip(weights, expected))


def portfolio_variance(weights: Sequence[float],
                       covariance: Sequence[Sequence[float]]) -> float:
    n = len(weights)
    if len(covariance) != n or any(len(row) != n for row in covariance):
        raise ValueError("Covariance matrix must be square and match weights")
    return sum(weights[i] * weights[j] * covariance[i][j]
               for i in range(n) for j in range(n))


def portfolio_duration(market_values: Sequence[float],
                       durations: Sequence[float]) -> float:
    if len(market_values) != len(durations) or sum(market_values) <= 0:
        raise ValueError("Positive market value and matching lengths required")
    total = sum(market_values)
    return sum(v * d for v, d in zip(market_values, durations)) / total


def tracking_error(active_returns: Sequence[float]) -> float:
    """Population standard deviation; annualisation is not assumed."""
    if not active_returns:
        raise ValueError("At least one active return needed")
    mean = sum(active_returns) / len(active_returns)
    return sqrt(sum((r - mean) ** 2 for r in active_returns) / len(active_returns))


def capm_required_return(risk_free: float, beta: float,
                         market_expected: float) -> float:
    return risk_free + beta * (market_expected - risk_free)


def credit_spread_approx(hazard_rate: float, recovery: float) -> float:
    """Heuristic for small intensities; NOT a complete credit pricing model."""
    if hazard_rate < 0 or not (0 <= recovery <= 1):
        raise ValueError("Invalid hazard rate or recovery fraction")
    return hazard_rate * (1 - recovery)


def one_period_default_price(payoff: float, default_probability: float,
                             recovery_fraction: float, risk_free: float) -> float:
    """Risk-neutral toy scenario; its probability must be risk-neutral."""
    if not (0 <= default_probability <= 1) or not (0 <= recovery_fraction <= 1):
        raise ValueError("Probability and recovery must be in [0,1]")
    expected = payoff * ((1-default_probability) +
                         default_probability*recovery_fraction)
    return expected / (1+risk_free)


def forward_delivery_price(spot: float, rate: float, years: int) -> float:
    """No-income asset with no storage costs; annual compounding."""
    return spot * (1+rate)**years


def call_option_one_step(spot_up: float, spot_down: float, strike: float,
                         q: float, risk_free: float) -> float:
    """Risk-neutral one-step binomial payoff valuation."""
    if not 0 <= q <= 1:
        raise ValueError("Risk-neutral probability must be within [0,1]")
    up = max(spot_up - strike, 0)
    down = max(spot_down - strike, 0)
    return (q*up+(1-q)*down)/(1+risk_free)


if __name__ == "__main__":
    cash = bond_cashflows()
    print("cashflows:", cash)
    print("price at 5%:", round(bond_price(.05), 4))
    print("price at 6%:", round(bond_price(.06), 4))
    print("modified duration:", round(modified_duration(cash, .05), 4))
    print("one-year forward 1→2:", round(forward_rate(.04, .05), 5))
    print("portfolio volatility:", round(sqrt(portfolio_variance(
        [.5, .5], [[.01, 0], [0, .04]])), 4))
