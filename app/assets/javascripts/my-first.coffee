MyOwnCtrl = ($scope, $http) ->
  $scope.listItems = []

  $http.get("/chat/users").success((data) ->
    $scope.listItems = ["sdfsdfsd", "sdfsdfsdfsdfs", "sdfsdfsdf"]
    $scope.selectedItem = $scope.listItems[0]
  )

ListCtrl = ($scope, $http) ->
  $scope.names = ['Dmitry', 'Alex']
  $scope.action = () ->
    alert("Link is pressed!")
  $http.get("test/good/4").success((data) ->
    $scope.g = data
  )

  $http.get("test/allgoods").success((data) ->
    $scope.allgoods = data
  )
