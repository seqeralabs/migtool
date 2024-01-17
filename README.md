# Mig Tool

Implements a no-frills DB migration tool inspired by Flyway. 

Migration files are a simple collection of SQL DDL/DML statements separated by `;` character, 
that can be stored either in a local directory or as resources in the application
classpath.
In addition, the migration files can be written in Groovy in order to run scripts using [`groovy-sql`](https://groovy-lang.org/databases.html).

Only requirement is that migration files follow the pattern `V99__Some_name.[sql|groovy]`, where 
`99` can be any integer value.

### Fixed and amended files

If you have broken migration scripts, you can use the migration tool to add fixed and amended files to existing migration files. These two files are always created together.

1. If the broken file has already been applied, a fixed file will be used. The fixed file name should follow this pattern:
   `V99_file_to_fix.fixed.[sql|groovy]`

2. If the broken file hasn't been applied, an amended file will be used instead of the original file. The amended file name should follow this pattern:
   `V99_file_to_fix.amended.[sql|groovy]`

> **Warning**
>
> The original file should always have both a fixed file and an amended file. It's not possible to have one without the other.

## Get started 

MigTool is designed to be embedded in the target app. Use the following idiom: 

```groovy
    MigTool tool = new MigTool();
            .withDriver('org.h2.Driver')
            .withDialect('h2')
            .withUrl('jdbc:h2:mem:test')
            .withUser('sa')
            .withPassword('')
            .withLocations('classpath:test')
      
    try {
        tool.run();
    }
    finally {
        tool.close();
    }
``` 

## Binary release 

MigTool is also released as a pre-compiled Linux binary including H2 and MySQL drivers. To use it, download the 
binary package at [this link](https://github.com/seqeralabs/migtool/releases/latest), then grant execute permission, 
finally run it following the example below:

```bash
./migtool \
   --username <username> \
   --password <password> \
   --url jdbc:mysql://<host>:3306/<schema> \
   --location file:/path/to/migration/scripts
```

> **Warning**
>
> Due to current limitations in GraalVM when it comes to running arbitrary Groovy scripts, the binary can only run SQL migration files.

                                                        
#### Options 

* `username`: Target database connection username.
* `password`: Target database connection password.
* `url`: Target database JDBC connection URL.
* `location`: File system path where migration scripts are located. Path must be prefixed with `file:`.
* `dialect`: Either `mysql` or `h2` (optional).
* `driver`: JDBC driver class name to connect the target database (optional).

## License 

[Mozilla Public License v2.0](LICENSE.txt)

