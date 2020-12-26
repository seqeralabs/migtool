# Mig Tool

Implements a no-frills DB migration tool inspired to Flyway. 

Migration files are a simple collection of DDL/MDL statements separated by `;` character, 
that can be stored either in a local directory or as resources in the application
classpath. 

Only requirement is that migration files follow the pattern `V99__Some_name`, where 
`99` can be one or more digits  integer value.


## Get started 

MigTool is designed to be embedded in the target app requiring. Use the following idiom: 

``` 
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

## License 

[Mozilla Public License v2.0](LICENSE.txt)

