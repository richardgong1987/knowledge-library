% Run from programming-languages/prolog/labs with: swipl -q -g main -t halt arithmetic-and-clpfd.pl
:- use_module(library(clpfd)).

% Level 3: is/2 evaluates its right-hand side, so X must already be a number.
plus_one(X, Y) :- Y is X + 1.

% Level 4: #= states a relation between integers, so it runs both ways.
plus_one_fd(X, Y) :- Y #= X + 1.

% Level 6: eight letters, eight different digits, one sum.
send_more_money([S,E,N,D,M,O,R,Y]) :-
    Digits = [S,E,N,D,M,O,R,Y],
    Digits ins 0..9,
    all_different(Digits),
    S #\= 0,
    M #\= 0,
    1000*S + 100*E + 10*N + D + 1000*M + 100*O + 10*R + E
        #= 10000*M + 1000*O + 100*N + 10*E + Y.

% Level 7: Qs lists, column by column, the row of each queen.
queens(N, Qs) :-
    length(Qs, N),
    Qs ins 1..N,
    safe(Qs).

safe([]).
safe([Q|Qs]) :- no_attack(Q, Qs, 1), safe(Qs).

% D is how many columns apart Q and Q1 are.
no_attack(_, [], _).
no_attack(Q, [Q1|Qs], D) :-
    Q #\= Q1,
    abs(Q - Q1) #\= D,
    D1 #= D + 1,
    no_attack(Q, Qs, D1).

% Three variables, two possible values: they cannot all differ.
three_bits(Constraint, Vs) :-
    Vs = [_, _, _],
    Vs ins 0..1,
    call(Constraint, Vs).

error_of(Goal, Error) :-
    catch((Goal, Error = none), error(Error, _), true).

main :-
    check_evaluation,
    check_comparison,
    check_one_way_is,
    check_relational_fd,
    check_domains_and_labeling,
    check_send_more_money,
    check_all_different,
    check_queens,
    format("OK~n").

% Level 1: = only unifies; is/2 evaluates.
check_evaluation :-
    T = 1 + 2,
    assertion(T == +(1, 2)),
    V is 1 + 2,
    assertion(V == 3),
    assertion(\+ 1 + 2 = 3),
    W is 1 + 2 * 3,
    assertion(W == 7),
    assertion(7 is 1 + 2 * 3),
    Half is 4 / 2,
    assertion(Half == 2),
    Big is 2 ** 100,
    assertion(Big == 1267650600228229401496703205376),
    current_prolog_flag(max_tagged_integer, MaxTagged),
    assertion(MaxTagged =:= 2 ** 56 - 1),
    assertion(7 / 2 =:= 3.5),
    assertion(7 // 2 =:= 3),
    assertion(-7 // 2 =:= -3),
    assertion(-7 div 2 =:= -4),
    assertion(-7 mod 2 =:= 1),
    assertion(-7 =:= 2 * (-7 div 2) + (-7 mod 2)).

% Level 2: comparison evaluates both sides; == compares terms.
check_comparison :-
    assertion(1 + 2 =:= 3),
    assertion(\+ 1 + 2 == 3),
    assertion(2 =:= 2.0),
    assertion(\+ 2 == 2.0),
    assertion(\+ 1 + 2 =\= 3),
    assertion(\+ 1 + 2 < 3),
    assertion(\+ 1 + 2 > 3),
    assertion(1 + 2 =< 3),
    assertion(1 + 2 >= 3).

% Level 3: forwards works, backwards raises an instantiation error.
check_one_way_is :-
    plus_one(3, Y),
    assertion(Y == 4),
    error_of(plus_one(_, 4), Error),
    assertion(Error == instantiation_error),
    assertion(\+ (X = 3, X is X + 1)).

% Level 4: #= runs in both directions and waits when both sides are unknown.
check_relational_fd :-
    plus_one_fd(3, Y),
    assertion(Y == 4),
    plus_one_fd(X, 4),
    assertion(X == 3),
    plus_one_fd(P, Q),
    copy_term([P, Q], [P1, Q1], Residual),
    assertion(Residual == [clpfd:(P1 + 1 #= Q1)]),
    P = 10,
    assertion(Q == 11).

% Level 5: domains shrink before labeling; labeling enumerates.
check_domains_and_labeling :-
    [A, B] ins 0..3,
    A + B #= 5,
    fd_dom(A, DomA),
    assertion(DomA == 2..3),
    findall(A-B, label([A, B]), Pairs),
    assertion(Pairs == [2-3, 3-2]),
    Z in 1..5,
    Z #> 3,
    fd_dom(Z, DomZ),
    assertion(DomZ == 4..5),
    findall(Down, (Down in 1..3, labeling([down], [Down])), Downs),
    assertion(Downs == [3, 2, 1]).

% Level 6: propagation alone fixes S, M and O; labeling finds the rest.
check_send_more_money :-
    send_more_money(Ds),
    Ds = [S, E, N, _, M, O, R, _],
    assertion(S-M-O == 9-1-0),
    fd_dom(E, DomE),
    fd_dom(N, DomN),
    fd_dom(R, DomR),
    assertion(DomE-DomN-DomR == (4..7)-(5..8)-(2..8)),
    findall(Ds, label(Ds), Solutions),
    assertion(Solutions == [[9,5,6,7,1,0,8,2]]),
    assertion(9567 + 1085 =:= 10652),
    assertion(10 * 9 * 8 * 7 * 6 * 5 * 4 * 3 =:= 1814400),
    assertion(4 * 4 * 7 * 7 * 7 =:= 5488).

% Interview: all_different/1 waits for values; all_distinct/1 also reasons about domains.
check_all_different :-
    assertion(three_bits(all_different, _)),
    assertion(\+ three_bits(all_distinct, _)),
    assertion(\+ (three_bits(all_different, Bits), label(Bits))).

% Level 7: 4 queens has 2 solutions, 8 queens has 92.
check_queens :-
    findall(Qs, (queens(4, Qs), label(Qs)), Four),
    assertion(Four == [[2,4,1,3], [3,1,4,2]]),
    once((queens(8, Q8), label(Q8))),
    assertion(Q8 == [1,5,8,6,3,7,2,4]),
    aggregate_all(count, (queens(8, Board), label(Board)), Count),
    assertion(Count == 92),
    assertion(8 ** 8 =:= 16777216).
