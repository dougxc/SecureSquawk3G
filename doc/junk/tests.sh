function go() {
    echo $1
    eval $1
}

go "squawk -verbose -cp:samples/j2meclasses java.lang.SuiteCreator -o test0 tests.Test0"
go "squawk -verbose -cp:samples/j2meclasses -suite:test0 java.lang.SuiteCreator -o test1 tests.Test1"
go "squawk -verbose -cp:samples/j2meclasses -suite:test1 java.lang.SuiteCreator -o test2 tests.Test2"
go "squawk -verbose -suite:test2 tests.Test2"
