"""Numerical and algebraic identities: simple checks, not theorem replacements.

All examples deliberately use Python's exact integer arithmetic where possible.
Every formula has an independent mathematical derivation in its Quarto lesson.
"""
from math import gcd, lcm, comb, isqrt, sqrt, ceil

def difference_of_squares(a,b):
    return a*a-b*b, (a+b)*(a-b)

def square_near(base,offset):
    return (base+offset)**2,base*base+2*base*offset+offset*offset

def cube_difference(a,b):
    return a**3-b**3,(a-b)*(a*a+a*b+b*b)

def difference_of_powers(a,b,n):
    return a**n-b**n,(a-b)*sum(a**(n-1-k)*b**k for k in range(n))

def arithmetic_sum(n):
    return n*(n+1)//2

def arithmetic_progression(a,d,n):
    return n*(2*a+(n-1)*d)//2

def geometric_sum(r,n):
    return sum(r**k for k in range(n))

def square_sum(n):
    return n*(n+1)*(2*n+1)//6

def telescoping_reciprocal(n):
    return sum(1/k-1/(k+1) for k in range(1,n+1))

def inclusive_range_sum(values,left,right):
    p=[0]
    for x in values:
        p.append(p[-1]+x)
    return p[right+1]-p[left]

def modular_power(base,exp,mod):
    if mod<=0:
        raise ValueError("Positive modulus required")
    value=1
    base%=mod
    while exp:
        if exp&1:
            value=value*base%mod
        base=base*base%mod
        exp//=2
    return value%mod

def gcd_lcm_identity(a,b):
    return gcd(a,b)*lcm(a,b),abs(a*b)

def ceil_nonnegative(a,b):
    if a<0 or b<=0:
        raise ValueError("Requires a>=0 and b>0")
    return (a+b-1)//b

def sum_digit_count(n):
    return sum(len(str(i)) for i in range(1,n+1))

def xor_unique(values):
    answer=0
    for x in values:
        answer^=x
    return answer

def count_set_bits(n):
    if n<0:
        raise ValueError("Requires n>=0")
    count=0
    while n:
        n&=n-1
        count+=1
    return count

def square_discriminant(a,b,c):
    return b*b-4*a*c

if __name__=="__main__":
    print("13²-7²:",difference_of_squares(13,7))
    print("98²:",square_near(100,-2))
    print("1+...+100:",arithmetic_sum(100))
    print("1²+...+10²:",square_sum(10))
    print("gcd*lcm:",gcd_lcm_identity(12,18))
    print("7^4 modulo 5:",modular_power(7,4,5))
    print("XOR singleton:",xor_unique([4,1,4,7,1]))
