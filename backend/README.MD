** AUTH **
http://localhost:8080/oauth2/authorization/google
http://localhost:8080/login/oauth2/code/google

** SWAGGER **
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs

** Sample Data **
On start application loads up sample data if database is empty.

** GENERATE FRONTEND FETCH CODE **
npx @rtk-query/codegen-openapi openapi-config.cts
