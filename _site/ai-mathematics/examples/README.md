# AI & Mathematics Lab

An **original** mathematical learning example linked across the AI & Mathematics chapters.

Our six invented shop orders have two features—delivery distance (km) and item count—and one response, fulfilment time (minutes). We use the same records to explain vector operations, optimisation, regression, PCA, Gaussian mixture models and a simple SVM separator.

## Run (Python 3.11+, no pip install)

~~~bash
python3 ai-mathematics/examples/delivery_ml_lab.py all
python3 ai-mathematics/examples/delivery_ml_lab.py regression
python3 ai-mathematics/examples/delivery_ml_lab.py pca
python3 ai-mathematics/examples/delivery_ml_lab.py gmm
python3 ai-mathematics/examples/delivery_ml_lab.py svm
python3 -m unittest discover -s ai-mathematics/examples -p 'test_*.py'
~~~

The chapter pages provide the mathematical derivations. This deliberately small script implements Gaussian elimination, normal-equation regression, batch gradient descent, two-feature PCA, a one-dimensional EM demonstration and the geometry of a specified linear SVM boundary. The SVM demo **does not train an optimised SVM**, and the EM demo is a two-Gaussian example rather than an industrial implementation.

Use a robust numerical library in real projects (for example, QR/SVD solvers instead of explicit normal equations). Avoid using these demonstrations to make real delivery or customer decisions.
