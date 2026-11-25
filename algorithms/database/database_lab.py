"""Database lab: runs every SQL query shown in the lesson on SQLite and checks the results.

Run from this folder with: python3 database_lab.py
"""

import sqlite3

TABLES = """
CREATE TABLE Person (personId INTEGER PRIMARY KEY, lastName TEXT, firstName TEXT);
INSERT INTO Person VALUES (1, 'Lee', 'Ana'), (2, 'Khan', 'Ben'), (3, 'Diaz', 'Cara');

CREATE TABLE Address (addressId INTEGER PRIMARY KEY, personId INTEGER, city TEXT, state TEXT);
INSERT INTO Address VALUES (1, 2, 'Leeds', 'England'), (2, 3, 'Cork', 'Munster'), (3, 9, 'Bath', 'England');

CREATE TABLE Department (id INTEGER PRIMARY KEY, name TEXT);
INSERT INTO Department VALUES (1, 'IT'), (2, 'Sales');

CREATE TABLE Employee (id INTEGER PRIMARY KEY, name TEXT, salary INTEGER, departmentId INTEGER);
INSERT INTO Employee VALUES
    (1, 'Ana', 90, 1), (2, 'Ben', 85, 1), (3, 'Cara', 90, 1), (4, 'Dev', 70, 1), (5, 'Eli', 60, 1),
    (6, 'Fay', 80, 2), (7, 'Gus', 60, 2);

CREATE TABLE Users (users_id INTEGER PRIMARY KEY, banned TEXT, role TEXT);
INSERT INTO Users VALUES
    (1, 'No', 'client'), (2, 'Yes', 'client'), (3, 'No', 'client'),
    (10, 'No', 'driver'), (11, 'No', 'driver'), (12, 'Yes', 'driver');

CREATE TABLE Trips (id INTEGER PRIMARY KEY, client_id INTEGER, driver_id INTEGER, city_id INTEGER,
                    status TEXT, request_at TEXT);
INSERT INTO Trips VALUES
    (1, 1, 10, 1, 'completed', '2013-10-01'),
    (2, 2, 11, 1, 'cancelled_by_driver', '2013-10-01'),
    (3, 3, 12, 6, 'completed', '2013-10-01'),
    (4, 3, 10, 6, 'cancelled_by_client', '2013-10-01'),
    (5, 1, 11, 1, 'completed', '2013-10-02'),
    (6, 3, 11, 6, 'completed', '2013-10-02'),
    (7, 1, 10, 1, 'cancelled_by_driver', '2013-10-03'),
    (8, 3, 11, 6, 'completed', '2013-10-03'),
    (9, 1, 10, 1, 'completed', '2013-10-03'),
    (10, 1, 10, 1, 'cancelled_by_client', '2013-10-04');
"""

# Level 1: select rows, then project columns.
SELECT_PROJECT = """
SELECT city
FROM Address
WHERE state = 'England';
"""

# Level 2: inner join and left join.
INNER_JOIN = """
SELECT p.firstName, a.city
FROM Person AS p
JOIN Address AS a ON a.personId = p.personId;
"""

CROSS_PRODUCT_SIZE = """
SELECT COUNT(*)
FROM Person, Address;
"""

# Level 3: grouping.
GROUP_BY_DEPARTMENT = """
SELECT departmentId, COUNT(*) AS staff, MAX(salary) AS top_salary
FROM Employee
GROUP BY departmentId;
"""

# Level 4: NULL and three-valued logic.
NULL_COMPARISON = """
SELECT p.firstName, a.city
FROM Person AS p
LEFT JOIN Address AS a ON a.personId = p.personId
WHERE a.city <> 'Leeds';
"""

NOT_IN_WITH_NULL = """
SELECT firstName
FROM Person
WHERE personId NOT IN (2, 3, NULL);
"""

COUNT_STAR_VERSUS_COLUMN = """
SELECT COUNT(*), COUNT(a.city)
FROM Person AS p
LEFT JOIN Address AS a ON a.personId = p.personId;
"""

# Level 5: three ways to number rows.
RANKINGS = """
SELECT name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) AS by_row,
       RANK()       OVER (ORDER BY salary DESC) AS by_rank,
       DENSE_RANK() OVER (ORDER BY salary DESC) AS by_dense_rank
FROM Employee
WHERE departmentId = 1;
"""

# Level 6: an index turns a scan into a search.
QUERY_PLAN = """
EXPLAIN QUERY PLAN
SELECT name FROM Employee WHERE departmentId = 2;
"""

CREATE_INDEX = """
CREATE INDEX idx_employee_department ON Employee(departmentId);
"""

# Interview 1: LeetCode 175, Combine Two Tables.
COMBINE_TWO_TABLES = """
SELECT p.firstName, p.lastName, a.city, a.state
FROM Person AS p
LEFT JOIN Address AS a ON a.personId = p.personId;
"""

# Interview 2: LeetCode 176, Second Highest Salary.
SECOND_HIGHEST_SALARY = """
SELECT (
    SELECT DISTINCT salary
    FROM Employee
    ORDER BY salary DESC
    LIMIT 1 OFFSET 1
) AS SecondHighestSalary;
"""

# Interview 3: LeetCode 185, Department Top Three Salaries (and LeetCode 184 with "= 1").
TOP_THREE_SALARIES = """
SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
FROM (
    SELECT name, salary, departmentId,
           DENSE_RANK() OVER (PARTITION BY departmentId ORDER BY salary DESC) AS salary_rank
    FROM Employee
) AS e
JOIN Department AS d ON d.id = e.departmentId
WHERE e.salary_rank <= 3;
"""

HIGHEST_SALARY = TOP_THREE_SALARIES.replace("e.salary_rank <= 3", "e.salary_rank = 1")

# Interview 4: LeetCode 262, Trips and Users.
TRIPS_AND_USERS = """
SELECT t.request_at AS Day,
       ROUND(AVG(CASE WHEN t.status = 'completed' THEN 0.0 ELSE 1.0 END), 2) AS "Cancellation Rate"
FROM Trips AS t
JOIN Users AS c ON c.users_id = t.client_id AND c.banned = 'No'
JOIN Users AS d ON d.users_id = t.driver_id AND d.banned = 'No'
WHERE t.request_at BETWEEN '2013-10-01' AND '2013-10-03'
GROUP BY t.request_at;
"""


def check(ok, what):
    if not ok:
        raise AssertionError(what)


def rows(db, sql):
    return sorted(db.execute(sql).fetchall(), key=repr)


def main():
    db = sqlite3.connect(":memory:")
    db.executescript(TABLES)

    # Level 1: sigma keeps 2 of the 3 Address rows, pi keeps one column.
    check(rows(db, SELECT_PROJECT) == [("Bath",), ("Leeds",)], "select and project")

    # Level 2: 3 x 3 = 9 pairs, 2 of them match; the left join adds Ana with NULLs.
    check(rows(db, CROSS_PRODUCT_SIZE) == [(9,)], "|Person x Address| = 9")
    check(rows(db, INNER_JOIN) == [("Ben", "Leeds"), ("Cara", "Cork")], "inner join keeps 2 rows")

    # Level 3: one row per department.
    check(rows(db, GROUP_BY_DEPARTMENT) == [(1, 5, 90), (2, 2, 80)], "group by department")

    # Level 4: NULL <> 'Leeds' is UNKNOWN, so Ana's row is dropped; NOT IN with a NULL keeps nothing.
    check(rows(db, NULL_COMPARISON) == [("Cara", "Cork")], "NULL comparison drops Ana")
    check(rows(db, NOT_IN_WITH_NULL) == [], "NOT IN (2, 3, NULL) is never TRUE")
    check(rows(db, NOT_IN_WITH_NULL.replace(", NULL", "")) == [("Ana",)], "NOT IN (2, 3) keeps Ana")
    check(rows(db, COUNT_STAR_VERSUS_COLUMN) == [(3, 2)], "COUNT(*) = 3, COUNT(city) = 2")

    # Level 5: the two 90s tie.
    ranked = sorted((salary, rank, dense) for _, salary, _, rank, dense in db.execute(RANKINGS))
    check(ranked == [(60, 5, 4), (70, 4, 3), (85, 3, 2), (90, 1, 1), (90, 1, 1)], "RANK 1,1,3,4,5 and DENSE_RANK 1,1,2,3,4")
    check(sorted(row[2] for row in db.execute(RANKINGS)) == [1, 2, 3, 4, 5], "ROW_NUMBER 1..5")

    # Level 6: the plan changes from SCAN to SEARCH ... USING INDEX.
    before = " ".join(row[-1] for row in db.execute(QUERY_PLAN))
    db.execute(CREATE_INDEX)
    after = " ".join(row[-1] for row in db.execute(QUERY_PLAN))
    check("SCAN" in before and "USING INDEX idx_employee_department" in after, f"plan: {before!r} -> {after!r}")
    check(rows(db, "SELECT name FROM Employee WHERE departmentId = 2;") == [("Fay",), ("Gus",)], "index gives same rows")
    height = 0
    while 100 ** height < 1_000_000:
        height += 1
    check(height == 3, "a B-tree with fan-out 100 over 10^6 keys has height 3")
    check(2 ** 19 < 1_000_000 <= 2 ** 20, "binary search over 10^6 keys: 20 steps")

    # Interview 1: LeetCode 175.
    check(rows(db, COMBINE_TWO_TABLES) == sorted([
        ("Ana", "Lee", None, None), ("Ben", "Khan", "Leeds", "England"), ("Cara", "Diaz", "Cork", "Munster"),
    ], key=repr), "LC 175: every person, with NULLs for Ana")

    # Interview 2: LeetCode 176.
    check(rows(db, SECOND_HIGHEST_SALARY) == [(85,)], "LC 176: distinct salaries 90, 85, ... -> 85")
    single = sqlite3.connect(":memory:")
    single.executescript("CREATE TABLE Employee (id INTEGER, salary INTEGER); INSERT INTO Employee VALUES (1, 100), (2, 100);")
    check(rows(single, SECOND_HIGHEST_SALARY) == [(None,)], "LC 176: only one distinct salary -> NULL")

    # Interview 3: LeetCode 185 and 184.
    check(rows(db, TOP_THREE_SALARIES) == sorted([
        ("IT", "Ana", 90), ("IT", "Cara", 90), ("IT", "Ben", 85), ("IT", "Dev", 70),
        ("Sales", "Fay", 80), ("Sales", "Gus", 60),
    ], key=repr), "LC 185: 4 IT rows from 3 distinct salaries, 2 Sales rows")
    check(rows(db, HIGHEST_SALARY) == sorted([("IT", "Ana", 90), ("IT", "Cara", 90), ("Sales", "Fay", 80)], key=repr),
          "LC 184: the top salary per department, ties included")

    # Interview 4: LeetCode 262.
    check(rows(db, TRIPS_AND_USERS) == [("2013-10-01", 0.5), ("2013-10-02", 0.0), ("2013-10-03", 0.33)],
          "LC 262: 1/2, 0/2, 1/3")
    joined = TRIPS_AND_USERS.split("FROM", 1)[1].split("WHERE")[0]
    check(rows(db, "SELECT COUNT(*) FROM" + joined) == [(8,)], "LC 262: the joins keep 8 of 10 trips")
    check(rows(db, "SELECT COUNT(*) FROM" + joined + "WHERE t.request_at BETWEEN '2013-10-01' AND '2013-10-03'") == [(7,)],
          "LC 262: the date filter keeps 7")

    print("OK")


if __name__ == "__main__":
    main()
