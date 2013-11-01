function ListCtrl($scope) {
    $scope.roots = [{name: "/", children: [
        {name: "node-1", children: [
            {name: "node-1.1"}
        ]},
        {name: "node-2", children: [
            {name: "node-2.1"}
        ]}
    ]}];
}
