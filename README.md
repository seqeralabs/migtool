# Mig Tool

Implements a no-frills DB migration tool inspired by Flyway. 

Migration files are a simple collection of SQL DDL/DML statements separated by `;` character, 
that can be stored either in a local directory or as resources in the application
classpath.
In addition, the migration files can be written in Groovy in order to run scripts using [`groovy-sql`](https://groovy-lang.org/databases.html).

Only requirement is that migration files follow the pattern `V99__Some_name.[sql|groovy]`, where 
`99` can be any integer value.

### Patch and override files

For each migration file that needs to be corrected a pair of `.patch.sql` and `.override.sql` must exist. 

The behaviour will follow these rules:
- **WHEN** file`.sql` exists and was executed **AND** file`.patch.sql`, file.override.sql pair exists -> apply patch
- **WHEN** file`.sql` exists and was not executed **AND** file`.patch.sql`, file`.override.sql` pair exists -> apply override

If for a given file there is only either a single .patch or a single .override then the tool will throw an illegal state exception stopping the migration process

## Get started 

MigTool is designed to be embedded in the target app. Use the following idiom: 

```groovy
    MigTool tool = new MigTool.Builder()
            .withDriver('org.h2.Driver')
            .withDialect('h2')
            .withUrl('jdbc:h2:mem:test')
            .withUser('sa')
            .withPassword('')
            .withLocations('classpath:test')
            .build()
      
    try {
        tool.run()
    }
    finally {
        tool.close()
    }
``` 

## Binary release 

MigTool is also released as a pre-compiled Linux binary including H2, MySQL and Postgres drivers. To use it, download the 
binary package at [this link](https://github.com/seqeralabs/migtool/releases/latest), then grant execute permission, finally run it following the example below:

```bash
./migtool \
   --username <username> \
   --password <password> \
   --url jdbc:<dialect>://<host>:3306/<schema> \
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
* `dialect`: Either `mysql`, `h2` or `postgres (postgresql)` (optional).
* `driver`: JDBC driver class name to connect the target database (optional).

In case the `driver` or `dialect` options are not provided, the tool will try to infer the driver class name from the `url` provided.

## License 

[Mozilla Public License v2.0](LICENSE.txt)

