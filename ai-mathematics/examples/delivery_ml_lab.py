#!/usr/bin/env python3
"""One original running example for AI & Mathematics: order fulfilment times.

Python 3.11+; standard library only.  All values are invented teaching data.
Run: python3 delivery_ml_lab.py all
     python3 delivery_ml_lab.py regression
"""
import math
import sys

# (delivery distance in km, number of items, fulfilment time in minutes)
ORDERS = [
    (1.0, 1.0, 7.0),
    (2.0, 2.0, 12.0),
    (3.0, 1.0, 11.0),
    (4.0, 3.0, 18.0),
    (5.0, 2.0, 17.0),
    (6.0, 3.0, 22.0),
]

def dot(a, b):
    return sum(x*y for x, y in zip(a, b))

def transpose(a):
    return [list(column) for column in zip(*a)]

def matmul(a, b):
    bt = transpose(b)
    return [[dot(row, col) for col in bt] for row in a]

def solve(a, b):
    """Gaussian elimination with partial pivoting (small full-rank examples)."""
    n = len(b)
    m = [list(map(float, row)) + [float(rhs)] for row, rhs in zip(a, b)]
    for col in range(n):
        pivot = max(range(col, n), key=lambda i: abs(m[i][col]))
        if abs(m[pivot][col]) < 1e-12:
            raise ValueError("Matrix singular or numerically near singular")
        m[col], m[pivot] = m[pivot], m[col]
        scale = m[col][col]
        for j in range(col, n + 1):
            m[col][j] /= scale
        for i in range(n):
            if i != col:
                factor = m[i][col]
                for j in range(col, n + 1):
                    m[i][j] -= factor * m[col][j]
    return [row[-1] for row in m]

def design_matrix():
    return [[1.0, distance, items] for distance, items, _ in ORDERS]

def labels():
    return [time for _, _, time in ORDERS]

def fit_ols():
    x, y = design_matrix(), labels()
    xt = transpose(x)
    gram = matmul(xt, x)
    rhs = [dot(row, y) for row in xt]
    return solve(gram, rhs)

def squared_error(beta):
    return sum((dot(row, beta) - y)**2 for row, y in zip(design_matrix(), labels()))

def gradient(beta):
    x, y = design_matrix(), labels()
    n = len(x)
    return [sum(2 * (dot(row, beta) - target) * row[j] / n
                for row, target in zip(x, y)) for j in range(3)]

def gradient_descent(steps=20000, rate=0.008):
    beta = [0.0, 0.0, 0.0]
    for _ in range(steps):
        g = gradient(beta)
        beta = [v - rate * d for v, d in zip(beta, g)]
    return beta

def principal_component():
    """PCA for two order features, without external numerical packages."""
    x = [[distance, items] for distance, items, _ in ORDERS]
    mx, my = [sum(row[j] for row in x)/len(x) for j in range(2)]
    centered = [[row[0]-mx, row[1]-my] for row in x]
    n = len(x)
    a = sum(row[0]**2 for row in centered) / (n-1)
    b = sum(row[0]*row[1] for row in centered) / (n-1)
    d = sum(row[1]**2 for row in centered) / (n-1)
    root = math.sqrt((a-d)**2 + 4*b*b)
    eigenvalue = (a+d+root)/2
    if abs(b)>1e-12:
        vec = [b, eigenvalue-a]
    else:
        vec = [1.0, 0.0] if a >= d else [0.0, 1.0]
    norm = math.hypot(*vec)
    direction = [z/norm for z in vec]
    scores = [dot(row, direction) for row in centered]
    return (mx, my), (a, b, d), eigenvalue, direction, scores

def normal_density(x, mean, variance):
    return math.exp(-0.5*(x-mean)**2/variance)/math.sqrt(2*math.pi*variance)

def gmm_em(iterations=30):
    """Two-component 1D Gaussian mixture on fulfilment times."""
    data=labels()
    weights=[0.5, 0.5]
    means=[9.0, 20.0]
    variances=[16.0, 16.0]
    for _ in range(iterations):
        responsibilities=[]
        for value in data:
            partial=[weights[k]*normal_density(value,means[k],variances[k]) for k in range(2)]
            total=sum(partial)
            responsibilities.append([p/total for p in partial])
        for k in range(2):
            nk=sum(r[k] for r in responsibilities)
            means[k]=sum(r[k]*x for r,x in zip(responsibilities,data))/nk
            variances[k]=max(0.2,sum(r[k]*(x-means[k])**2
                              for r,x in zip(responsibilities,data))/nk)
            weights[k]=nk/len(data)
    return weights,means,variances

def svm_toy():
    """A chosen separating plane for an invented late-delivery label."""
    # For this dataset, the true label is late iff distance >= 4 km.
    w=(2.0, 0.0)
    bias=-7.0
    margins=[]
    for distance,items,_ in ORDERS:
        label=1 if distance>=4 else -1
        functional_margin=label*(w[0]*distance+w[1]*items+bias)
        margins.append(functional_margin)
    geometric_margin=min(margins)/math.hypot(*w)
    return margins,geometric_margin

def show_data():
    print("Order dataset: (distance km, items, fulfilment min)")
    for item in ORDERS:
        print("  ",item)

def demo_linear():
    x=design_matrix()
    print("Design matrix shape:",len(x),"x",len(x[0]))
    print("First feature row:",x[0])
    print("Normal equations (X^T X) beta = X^T y")
    print("Least-squares beta:",[round(v,5) for v in fit_ols()])

def demo_geometry():
    a,b=[2.,1.],[4.,3.]
    diff=[z-y for z,y in zip(b,a)]
    print("Distance between orders [2,1] and [4,3]:",round(math.hypot(*diff),5))
    print("Dot product:",dot(a,b))
    proj=[dot(a,b)/dot(b,b)*z for z in b]
    print("Projection of first onto second:",[round(v,5) for v in proj])

def demo_calculus():
    beta=[0.,0.,0.]
    print("Squared loss at origin:",squared_error(beta))
    print("Gradient of mean-squared-error at origin:",
          [round(z,5) for z in gradient(beta)])

def demo_probability():
    late=[distance>=4 for distance,_,_ in ORDERS]
    print("Empirical P(late):",sum(late)/len(late))
    print("P(late | distance >= 4):",sum(late)/sum(distance>=4 for distance,_,_ in ORDERS))

def demo_optimisation():
    beta=gradient_descent()
    print("Gradient descent beta:",[round(v,5) for v in beta])
    print("Least-squares beta:",[round(v,5) for v in fit_ols()])
    print("Training SSE:",round(squared_error(beta),5))

def demo_regression():
    beta=fit_ols()
    x=[1.,7.,2.]
    print("Prediction for 7 km, 2 items:",round(dot(x,beta),3),"minutes")

def demo_pca():
    mean,cov,eigenvalue,direction,scores=principal_component()
    print("Centroid:",mean,"Covariance:",cov)
    print("Largest eigenvalue:",round(eigenvalue,5))
    print("Principal direction:",[round(v,5) for v in direction])
    print("First 3 projected scores:",[round(v,5) for v in scores[:3]])

def demo_gmm():
    weights,means,variances=gmm_em()
    print("Two-component mixture weights:",[round(v,4) for v in weights])
    print("Means:",[round(v,4) for v in means])
    print("Variances:",[round(v,4) for v in variances])

def demo_svm():
    margins,geometric_margin=svm_toy()
    print("Functional margins:",[round(v,3) for v in margins])
    print("Minimum geometric margin:",round(geometric_margin,5))

DEMOS = {
    "data":show_data,"linear":demo_linear,"geometry":demo_geometry,
    "calculus":demo_calculus,"probability":demo_probability,
    "optimisation":demo_optimisation,"regression":demo_regression,
    "pca":demo_pca,"gmm":demo_gmm,"svm":demo_svm,
}

def main():
    selected=sys.argv[1] if len(sys.argv)>1 else "all"
    if selected=="all":
        for name,run in DEMOS.items():
            print("\n==",name,"==")
            run()
    elif selected in DEMOS:
        DEMOS[selected]()
    else:
        raise SystemExit("Available demos: all, "+", ".join(DEMOS))
if __name__=="__main__":
    main()
