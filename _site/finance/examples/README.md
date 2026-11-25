# Finance: tiny original calculation lab

All examples use **annual compounding** and decimal interest rates unless stated otherwise. `0.05` means 5%, not 0.05%. **One basis point = 0.0001**.

From the repository root:

```bash
python3 finance/examples/finance_lab.py
python3 -m unittest discover -s finance/examples -p 'test_*.py'
```

The [original single-file module](finance_lab.py) illustrates present value, a two-year coupon bond, YTM, duration, convexity, DV01, spot curves, forwards, bootstrap, clean/dirty price, portfolios, default models, CAPM and an elementary option model. [Tests](test_finance_lab.py) verify the numerical results.

The library is a **learning aid**, not a trading engine. Real fixed-income production code must handle market-specific coupon calendars, business-day schedules, day-count conventions, settlement lags, compounding, accrued interest, callable cash flows, credit modelling, curve calibration and numerical tolerances.

No material is copied from the provided books. The ideas are independently explained and attributed in each chapter.
