% Run from programming-languages/prolog/labs with:
%   swipl -q -g main -t halt unification.pl

% Level 2: unification in a clause head takes a term apart, or builds one.
date_month(date(_, Month, _), Month).

level1_constants_and_variables :-
    assertion(tom = tom),
    assertion(\+ tom = bob),
    assertion(\+ 1 = 1.0),        % different types never unify
    assertion(1 =:= 1.0),         % arithmetic comparison is a different question
    X = tom,
    assertion(X == tom).

level2_compound_terms :-
    point(X, 2) = point(1, Y),
    assertion(X-Y == 1-2),
    date(2026, M, 26) = date(Year, 11, D),
    assertion([Year, M, D] == [2026, 11, 26]),
    assertion(\+ f(a) = g(a)),    % functor clash
    assertion(\+ f(a) = f(a, b)), % arity clash
    assertion(\+ f(Z, Z) = f(a, b)),
    date_month(date(2026, 11, 26), Month),
    assertion(Month == 11),
    date_month(Date, 11),
    assertion(Date = date(_, 11, _)).

% Level 3: the most general unifier.
level3_mgu :-
    f(X, g(Y)) = f(a, g(X)),
    assertion(X-Y == a-a),
    point(P, Q) = point(Q, 3),
    assertion(P-Q == 3-3),
    f(A) = f(B),                  % mgu {A -> B}: both stay unbound, now aliased
    assertion((var(A), A == B)).

% Level 4: = unifies, == compares without binding, \= means "cannot unify".
level4_equality :-
    assertion(\+ _ == _),         % two different fresh variables
    X = Y,
    assertion(X == Y),
    assertion(a \= b),
    assertion(\+ _ \= a),         % an unbound variable can unify with a
    assertion(\+ (V \= a, V = b)),
    assertion((W = b, W \= a)),   % same goals, other order: now it succeeds
    dif(Z, a),                    % dif waits until Z is known
    assertion(\+ Z = a),
    Z = b,
    assertion(Z == b).

% Level 5: the occurs check.
level5_occurs_check :-
    X = f(X),                     % succeeds in SWI-Prolog: a cyclic term
    assertion(cyclic_term(X)),
    assertion(\+ unify_with_occurs_check(Y, f(Y))),
    assertion(\+ unify_with_occurs_check(f(B, g(B)), f(C, C))),
    Z = f(f(Z)),
    assertion(X == Z).            % both are the infinite tree f(f(f(...)))

occurs_check_flag :-
    set_prolog_flag(occurs_check, true),
    assertion(\+ X = f(X)),
    set_prolog_flag(occurs_check, error),
    catch(Y = f(Y), Error, true),
    assertion(Error = error(occurs_check(_, _), _)),
    set_prolog_flag(occurs_check, false).

main :-
    level1_constants_and_variables,
    level2_compound_terms,
    level3_mgu,
    level4_equality,
    level5_occurs_check,
    occurs_check_flag,
    format("OK~n").
