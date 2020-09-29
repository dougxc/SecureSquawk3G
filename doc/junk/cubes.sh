function go() {
    echo $1
    eval $1
}

go "squawk -verbose -cp:graphics/j2meclasses java.lang.SuiteCreator -o graphics '*'"
go "squawk -verbose -cp:samples/j2meclasses -suite:graphics java.lang.SuiteCreator -o cubes example.cubes"
go "squawk -verbose -suite:cubes example.cubes.Main"
