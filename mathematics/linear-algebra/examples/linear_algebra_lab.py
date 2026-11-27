"""One-file numerical checks for our original linear algebra lessons.

Only Python's standard library. The chapters explain WHY calculations work;
this file merely verifies deliberately small exact or floating-point examples.
"""

def transform(x, y):
    return (2*x+y, x-y)


def add(u, v):
    return tuple(a+b for a,b in zip(u,v))


def dot(u,v):
    return sum(a*b for a,b in zip(u,v))


def matrix_vector(A,v):
    return tuple(sum(row[j]*v[j] for j in range(len(v))) for row in A)


def determinant2(A):
    return A[0][0]*A[1][1]-A[0][1]*A[1][0]


def eliminate_example():
    # x+y=7; 2x+y=8
    x=8-7
    return (x,7-x)


def projection(v,u):
    factor=dot(v,u)/dot(u,u)
    return tuple(factor*x for x in u)


def gaussian_fit_constant(values):
    return sum(values)/len(values)


def transition(P,p):
    return matrix_vector(P,p)


def matrix_product(A,B):
    return tuple(tuple(sum(A[i][k]*B[k][j] for k in range(len(B)))
                       for j in range(len(B[0]))) for i in range(len(A)))


def square(A):
    return matrix_product(A,A)


def norm_squared(v):
    return sum(abs(z)**2 for z in v)


def rotate90(v):
    return (-v[1],v[0])


def gram_schmidt_second(first,second):
    scale=dot(second,first)/dot(first,first)
    return tuple(v-scale*u for v,u in zip(second,first))


if __name__=="__main__":
    print("T(3,2) =", transform(3,2))
    print("vector sum =", add((3,2),(-1,4)))
    print("dot product =", dot((3,1),(1,-3)))
    print("determinant =", determinant2(((2,1),(1,-1))))
    print("equation solution =", eliminate_example())
    print("projection =",projection((3,4),(1,0)))
    print("rotation =",rotate90((3,2)))
    print("next-state probabilities =",transition(((.9,.5),(.1,.5)),(.8,.2)))
