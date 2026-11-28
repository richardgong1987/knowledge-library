"""Pure-Python numerical checks for interview mathematical invariants.

No external dependencies and no copied LeetCode solutions. These tiny
functions validate illustrative arithmetic and simple invariant claims;
the proof lives in the corresponding Quarto chapter.
"""
from collections import deque
from math import gcd, comb


def prefix_sums(nums):
    p=[0]
    for x in nums:
        p.append(p[-1]+x)
    return p


def range_sum(nums,left,right):
    p=prefix_sums(nums)
    return p[right+1]-p[left]


def count_target_subarrays(nums,target):
    seen={0:1}
    p=answer=0
    for value in nums:
        p+=value
        answer+=seen.get(p-target,0)
        seen[p]=seen.get(p,0)+1
    return answer


def fixed_windows(nums,size):
    if not 0<size<=len(nums):
        raise ValueError("Size must fit input")
    w=sum(nums[:size])
    result=[w]
    for i in range(len(nums)-size):
        w=w-nums[i]+nums[i+size]
        result.append(w)
    return result


def add_interval(length,left,right,value):
    d=[0]*(length+1)
    d[left]+=value
    d[right+1]-=value
    result=[]
    total=0
    for k in range(length):
        total+=d[k]
        result.append(total)
    return result


def min_days(total,per_day):
    lo,hi=0,total
    while lo<hi:
        mid=(lo+hi)//2
        if mid*per_day>=total:
            hi=mid
        else:
            lo=mid+1
    return lo


def climb(n):
    prev,curr=1,1
    for _ in range(2,n+1):
        prev,curr=curr,prev+curr
    return curr


def knapsack_01(capacity,items):
    dp=[0]*(capacity+1)
    for weight,value in items:
        for c in range(capacity,weight-1,-1):
            dp[c]=max(dp[c],dp[c-weight]+value)
    return dp[capacity]


def lis_length(nums):
    from bisect import bisect_left
    tails=[]
    for x in nums:
        p=bisect_left(tails,x)
        if p==len(tails):
            tails.append(x)
        else:
            tails[p]=x
    return len(tails)


def count_inclusion_exclusion(a,b,intersection):
    return a+b-intersection


def number_of_subarrays(n):
    return n*(n+1)//2


def dijkstra_small_graph():
    # s->a (3), s->b (9), a->b (4)
    d={"s":0,"a":float("inf"),"b":float("inf")}
    for u,v,w in [("s","a",3),("s","b",9),("a","b",4)]:
        d[v]=min(d[v],d[u]+w)
    return d["b"]


def kadane_nonempty(nums):
    cur=best=nums[0]
    for x in nums[1:]:
        cur=max(x,cur+x)
        best=max(best,cur)
    return best


def polynomial_hash(codes,base=3,mod=101):
    return sum(c*base**i for i,c in enumerate(codes))%mod


def interval_optimum(intervals):
    """Classic unweighted interval scheduling on half-open time intervals."""
    answer=0
    last_end=float("-inf")
    for start,end in sorted(intervals,key=lambda a:a[1]):
        if start>=last_end:
            last_end=end
            answer+=1
    return answer


def bfs_shortest_path():
    edges={"A":["B","C"],"B":["D"],"C":[],"D":[]}
    q=deque([("A",0)])
    visited={"A"}
    while q:
        u,d=q.popleft()
        if u=="D":
            return d
        for v in edges[u]:
            if v not in visited:
                visited.add(v)
                q.append((v,d+1))


if __name__=="__main__":
    print("prefix:",range_sum([2,4,1,5],1,3))
    print("subarray count:",count_target_subarrays([1,2,1],3))
    print("sliding:",fixed_windows([2,4,1,5],3))
    print("DP:",climb(3),knapsack_01(3,[(2,3),(3,4)]))
    print("Kadane:",kadane_nonempty([-2,3,-1,4]))
