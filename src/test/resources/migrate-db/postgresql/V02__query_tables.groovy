
def orgs = sql.rows("SELECT * FROM organization")
def licenses = sql.rows("SELECT * FROM license")

println "Orgs: ${orgs}"
println "Licenses: ${licenses}"