# JSQLON

Write SQL, read JSON.

## Usage

    $ jsqlon <JDBC connection string>

Write semicolon-terminated SQL to STDIN. Read JSON from STDOUT.

For example, with a PostgreSQL table named `people` with four columns: 

  * `id INT`
  * `name TEXT`
  * `dob DATE` 
  * `meta JSON`

```
$ jsqlon 'postgresql://localhost/people?user=me&password=knockknock'
SELECT * FROM people
[{"id": 1, "name": "Alice", "dob": "1978-02-01", "meta": null},
 {"id": 2, "name": "Bob",   "dob": null,         "meta": {"talks-to": "Alice", "height": 187}}]
```

## License

This software is licensed under the [GNU General Public License, version 3][].

[GNU General Public License, version 3]: https://www.gnu.org/licenses/gpl-3.0.en.html
